package com.picknpay.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picknpay.ecommerce.config.SecurityConfig;
import com.picknpay.ecommerce.dto.request.CreateProductRequest;
import com.picknpay.ecommerce.dto.response.ProductResponse;
import com.picknpay.ecommerce.exception.DuplicateSkuException;
import com.picknpay.ecommerce.exception.ResourceNotFoundException;
import com.picknpay.ecommerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=admin",
        "app.security.password=secret"
})
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;

    private ProductResponse sample() {
        return ProductResponse.builder()
                .id(1L).name("Eggs").description("d")
                .price(3999).priceFormatted("R39.99")
                .stockQuantity(100).sku("SKU-EGGS")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /products returns paginated content (public)")
    void list_publicAndPaginated() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sample()));
        when(productService.getProducts(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-EGGS"))
                .andExpect(jsonPath("$.content[0].priceFormatted").value("R39.99"));
    }

    @Test
    @DisplayName("GET /products/{id} returns the product (public)")
    void getOne_found() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-EGGS"));
    }

    @Test
    @DisplayName("GET /products/{id} returns 404 with error envelope when not found")
    void getOne_notFound() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product with ID 99 was not found"));
    }

    @Test
    @DisplayName("POST /products without auth returns 401")
    void create_noAuth_unauthorized() throws Exception {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("X").price(100).stockQuantity(1).sku("SKU-X").build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /products with valid body and auth returns 201")
    void create_happy() throws Exception {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Eggs").price(3999).stockQuantity(100).sku("SKU-EGGS").build();
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.priceFormatted").value("R39.99"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /products with invalid body returns 400 + fieldErrors")
    void create_invalid_returns400() throws Exception {
        String body = """
                {"name":"", "price":-1, "stockQuantity":-1, "sku":""}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.sku").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /products with duplicate SKU returns 409")
    void create_duplicateSku_returns409() throws Exception {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Eggs").price(3999).stockQuantity(100).sku("SKU-EGGS").build();
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenThrow(new DuplicateSkuException("SKU-EGGS"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_SKU"));
    }
}
