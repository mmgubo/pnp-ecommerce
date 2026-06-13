package com.picknpay.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picknpay.ecommerce.config.SecurityConfig;
import com.picknpay.ecommerce.dto.request.OrderItemRequest;
import com.picknpay.ecommerce.dto.request.PlaceOrderRequest;
import com.picknpay.ecommerce.dto.response.OrderItemResponse;
import com.picknpay.ecommerce.dto.response.OrderResponse;
import com.picknpay.ecommerce.enums.OrderStatus;
import com.picknpay.ecommerce.exception.InsufficientStockException;
import com.picknpay.ecommerce.exception.InvalidOrderStateException;
import com.picknpay.ecommerce.exception.ResourceNotFoundException;
import com.picknpay.ecommerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=admin",
        "app.security.password=secret"
})
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OrderService orderService;

    private OrderResponse sampleOrder(OrderStatus status) {
        OrderItemResponse item = OrderItemResponse.builder()
                .id(1L).productId(1L).productName("Eggs").productSku("SKU-EGGS")
                .quantity(3).unitPrice(3999).unitPriceFormatted("R39.99")
                .lineTotal(11997L).lineTotalFormatted("R119.97").build();
        return OrderResponse.builder()
                .id(42L).customerName("L").customerEmail("l@example.com")
                .status(status).totalAmount(11997L).totalAmountFormatted("R119.97")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .items(List.of(item)).build();
    }

    private PlaceOrderRequest validRequest() {
        return PlaceOrderRequest.builder()
                .customerName("L").customerEmail("l@example.com")
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(3).build()))
                .build();
    }

    @Test
    @DisplayName("POST /orders without auth returns 401")
    void place_noAuth() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders with valid body returns 201 CONFIRMED")
    void place_happy() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(sampleOrder(OrderStatus.CONFIRMED));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(3999));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders with empty items returns 400")
    void place_emptyItems_returns400() throws Exception {
        String body = """
                {"customerName":"L","customerEmail":"l@example.com","items":[]}
                """;
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders with invalid email returns 400")
    void place_invalidEmail_returns400() throws Exception {
        String body = """
                {"customerName":"L","customerEmail":"not-an-email","items":[{"productId":1,"quantity":1}]}
                """;
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerEmail").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders with insufficient stock returns 409")
    void place_insufficientStock_returns409() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderRequest.class)))
                .thenThrow(new InsufficientStockException("SKU-EGGS", 9999, 100));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /orders/{id} returns the order when authenticated")
    void getOne_found() throws Exception {
        when(orderService.getOrderById(42L)).thenReturn(sampleOrder(OrderStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/orders/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /orders/{id} returns 404 when not found")
    void getOne_notFound() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /orders/{id} without auth returns 401 (PII protection)")
    void getOne_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /orders/{id}/cancel returns the cancelled order")
    void cancel_happy() throws Exception {
        when(orderService.cancelOrder(42L)).thenReturn(sampleOrder(OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/orders/42/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /orders/{id}/cancel on a cancelled order returns 409")
    void cancel_invalidState_returns409() throws Exception {
        when(orderService.cancelOrder(42L)).thenThrow(
                new InvalidOrderStateException(42L, "CANCELLED", "cancel"));

        mockMvc.perform(patch("/api/v1/orders/42/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_ORDER_STATE"));
    }

    @Test
    @DisplayName("PATCH /orders/{id}/cancel without auth returns 401")
    void cancel_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/42/cancel"))
                .andExpect(status().isUnauthorized());
    }
}
