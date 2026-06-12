package com.picknpay.ecommerce.dto.response;

import com.picknpay.ecommerce.enums.OrderStatus;
import com.picknpay.ecommerce.model.Order;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Value
@Builder
public class OrderResponse {
    Long id;
    String customerName;
    String customerEmail;
    OrderStatus status;
    Long totalAmount;
    String totalAmountFormatted;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalAmountFormatted(String.format(Locale.UK, "R%.2f", order.getTotalAmount() / 100.0))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                .build();
    }
}
