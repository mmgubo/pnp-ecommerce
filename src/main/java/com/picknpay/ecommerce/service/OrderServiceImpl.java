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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        log.info("Placing order for customer={} items={}",
                request.getCustomerEmail(), request.getItems().size());

        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(0L)
                .items(new ArrayList<>())
                .build();

        long total = 0L;
        for (OrderItemRequest line : request.getItems()) {
            // Pessimistic write lock: SELECT ... FOR UPDATE prevents two concurrent
            // orders from both seeing sufficient stock and driving inventory negative.
            Product product = productRepository.findByIdWithLock(line.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", line.getProductId()));

            if (product.getStockQuantity() < line.getQuantity()) {
                throw new InsufficientStockException(
                        product.getSku(), line.getQuantity(), product.getStockQuantity());
            }
            product.decrementStock(line.getQuantity());

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(line.getQuantity())
                    .unitPrice(product.getPrice())  // snapshot at order time
                    .build();
            order.addItem(item);

            total += (long) product.getPrice() * line.getQuantity();
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        log.info("Order {} placed, totalAmount={} cents", saved.getId(), saved.getTotalAmount());
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(id, order.getStatus().name(), "cancel");
        }

        for (OrderItem item : order.getItems()) {
            // Re-lock the product before mutating stock to maintain the same concurrency
            // invariant as placeOrder — no read-modify-write races on stock.
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProduct().getId()));
            product.restoreStock(item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);

        log.info("Order {} cancelled, stock restored across {} items",
                order.getId(), order.getItems().size());
        return OrderResponse.from(order);
    }
}
