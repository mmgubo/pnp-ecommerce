package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.request.OrderItemRequest;
import com.picknpay.ecommerce.dto.request.PlaceOrderRequest;
import com.picknpay.ecommerce.dto.response.OrderResponse;
import com.picknpay.ecommerce.enums.OrderStatus;
import com.picknpay.ecommerce.exception.InsufficientStockException;
import com.picknpay.ecommerce.exception.InvalidOrderStateException;
import com.picknpay.ecommerce.exception.ResourceNotFoundException;
import com.picknpay.ecommerce.model.Order;
import com.picknpay.ecommerce.model.OrderItem;
import com.picknpay.ecommerce.model.Product;
import com.picknpay.ecommerce.repository.OrderRepository;
import com.picknpay.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository   orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderServiceImpl orderService;

    private Product eggs;
    private Product milk;

    @BeforeEach
    void setUp() {
        eggs = Product.builder().id(1L).name("Eggs").sku("SKU-EGGS").price(3999).stockQuantity(100).build();
        milk = Product.builder().id(2L).name("Milk").sku("SKU-MILK").price(2999).stockQuantity(50).build();
    }

    @Test
    @DisplayName("placeOrder decrements stock, snapshots unit_price, and computes total")
    void placeOrder_happy() {
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(eggs));
        when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(milk));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        PlaceOrderRequest req = PlaceOrderRequest.builder()
                .customerName("L").customerEmail("l@example.com")
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(3).build(),
                        OrderItemRequest.builder().productId(2L).quantity(2).build()))
                .build();

        OrderResponse out = orderService.placeOrder(req);

        assertThat(out.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(out.getTotalAmount()).isEqualTo(3L * 3999 + 2L * 2999);
        assertThat(eggs.getStockQuantity()).isEqualTo(97);
        assertThat(milk.getStockQuantity()).isEqualTo(48);
        assertThat(out.getItems()).hasSize(2);
        assertThat(out.getItems().get(0).getUnitPrice()).isEqualTo(3999);
    }

    @Test
    @DisplayName("placeOrder throws InsufficientStockException when quantity exceeds available stock")
    void placeOrder_insufficientStock() {
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(eggs));

        PlaceOrderRequest req = PlaceOrderRequest.builder()
                .customerName("L").customerEmail("l@example.com")
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(9999).build()))
                .build();

        assertThatThrownBy(() -> orderService.placeOrder(req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("SKU-EGGS")
                .hasMessageContaining("9999");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeOrder throws ResourceNotFoundException when productId does not exist")
    void placeOrder_productNotFound() {
        when(productRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        PlaceOrderRequest req = PlaceOrderRequest.builder()
                .customerName("L").customerEmail("l@example.com")
                .items(List.of(OrderItemRequest.builder().productId(999L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> orderService.placeOrder(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder restores stock per item and sets status to CANCELLED")
    void cancelOrder_happy() {
        Order order = buildConfirmedOrderWithItem(eggs, 5);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(eggs));

        OrderResponse out = orderService.cancelOrder(10L);

        assertThat(out.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(eggs.getStockQuantity()).isEqualTo(105);
    }

    @Test
    @DisplayName("cancelOrder throws InvalidOrderStateException for an already-cancelled order")
    void cancelOrder_alreadyCancelled() {
        Order order = buildConfirmedOrderWithItem(eggs, 5);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(10L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder throws ResourceNotFoundException for unknown order ID")
    void cancelOrder_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById returns a mapped response when the order exists")
    void getOrderById_happy() {
        Order order = buildConfirmedOrderWithItem(eggs, 2);
        order.setId(7L);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        OrderResponse out = orderService.getOrderById(7L);

        assertThat(out.getId()).isEqualTo(7L);
        assertThat(out.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getOrderById throws ResourceNotFoundException for unknown ID")
    void getOrderById_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Order buildConfirmedOrderWithItem(Product product, int qty) {
        OrderItem item = OrderItem.builder()
                .product(product).quantity(qty).unitPrice(product.getPrice()).build();
        Order order = Order.builder()
                .id(10L)
                .customerName("L").customerEmail("l@example.com")
                .status(OrderStatus.CONFIRMED)
                .totalAmount((long) product.getPrice() * qty)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
        return order;
    }
}
