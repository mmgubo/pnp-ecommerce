package com.picknpay.ecommerce.controller;

import com.picknpay.ecommerce.enums.OrderStatus;
import com.picknpay.ecommerce.model.Order;
import com.picknpay.ecommerce.model.OrderItem;
import com.picknpay.ecommerce.model.Product;
import com.picknpay.ecommerce.repository.OrderRepository;
import com.picknpay.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test for the reporting endpoints. Boots the complete
 * Spring context against H2 and asserts the aggregate SQL queries produce
 * correct results against real data.
 *
 * DirtiesContext recreates the context (and schema, since H2 is in-memory)
 * before each test so test fixtures stay isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@WithMockUser  // reports are authenticated as of v1.1 — commercially sensitive aggregates
class ReportControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository   orderRepository;

    private Product eggs;
    private Product milk;

    @BeforeEach
    void seed() {
        // Use distinct SKUs so the seed catalogue does not collide.
        eggs = productRepository.save(Product.builder()
                .name("Test Eggs").sku("T-EGGS").price(3999).stockQuantity(100).build());
        milk = productRepository.save(Product.builder()
                .name("Test Milk").sku("T-MILK").price(2999).stockQuantity(100).build());

        // CONFIRMED: eggs x 5, milk x 2 (two separate orders)
        placeOrder(eggs, 5, OrderStatus.CONFIRMED);
        placeOrder(milk, 2, OrderStatus.CONFIRMED);

        // CANCELLED: eggs x 99 — must NOT inflate top-products totals
        placeOrder(eggs, 99, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("top-products ranks by total units (CONFIRMED only)")
    void topProducts_rankedExcludingCancelled() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productSku=='T-EGGS')].totalUnitsSold").value(5))
                .andExpect(jsonPath("$[?(@.productSku=='T-EGGS')].orderCount").value(1))
                .andExpect(jsonPath("$[?(@.productSku=='T-MILK')].totalUnitsSold").value(2));
    }

    @Test
    @DisplayName("top-products with from=tomorrow returns no rows")
    void topProducts_futureFrom_returnsEmpty() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/v1/reports/top-products")
                        .param("limit", "5")
                        .param("from", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("top-products with from=yesterday includes today's orders")
    void topProducts_yesterdayFrom_includesToday() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/api/v1/reports/top-products")
                        .param("limit", "5")
                        .param("from", yesterday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productSku=='T-EGGS')].totalUnitsSold").value(5))
                .andExpect(jsonPath("$[?(@.productSku=='T-MILK')].totalUnitsSold").value(2));
    }

    @Test
    @DisplayName("order-summary groups counts and revenue by status")
    void orderSummary_groupsByStatus() throws Exception {
        // CONFIRMED total: (5 * 3999) + (2 * 2999) = 19995 + 5998 = 25993 cents
        // CANCELLED total: 99 * 3999 = 395901 cents
        mockMvc.perform(get("/api/v1/reports/order-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status=='CONFIRMED')].orderCount").value(2))
                .andExpect(jsonPath("$[?(@.status=='CONFIRMED')].totalRevenue").value(25993))
                .andExpect(jsonPath("$[?(@.status=='CANCELLED')].orderCount").value(1))
                .andExpect(jsonPath("$[?(@.status=='CANCELLED')].totalRevenue").value(395901));
    }

    @Test
    @DisplayName("top-products returns 400 when limit exceeds 50")
    void topProducts_limitExceeds50_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.limit").exists());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void placeOrder(Product product, int qty, OrderStatus status) {
        OrderItem item = OrderItem.builder()
                .product(product).quantity(qty).unitPrice(product.getPrice()).build();
        Order order = Order.builder()
                .customerName("Tester").customerEmail("test@example.com")
                .status(status)
                .totalAmount((long) product.getPrice() * qty)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
        orderRepository.save(order);
    }
}
