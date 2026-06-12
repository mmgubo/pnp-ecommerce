package com.picknpay.ecommerce.dto.response;

import com.picknpay.ecommerce.model.OrderItem;
import lombok.Builder;
import lombok.Value;

import java.util.Locale;

@Value
@Builder
public class OrderItemResponse {
    Long id;
    Long productId;
    String productName;
    String productSku;
    Integer quantity;
    Integer unitPrice;
    String unitPriceFormatted;
    Long lineTotal;
    String lineTotalFormatted;

    public static OrderItemResponse from(OrderItem item) {
        long total = (long) item.getUnitPrice() * item.getQuantity();
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .unitPriceFormatted(formatRand(item.getUnitPrice()))
                .lineTotal(total)
                .lineTotalFormatted(formatRand(total))
                .build();
    }

    private static String formatRand(long cents) {
        return String.format(Locale.UK, "R%.2f", cents / 100.0);
    }
}
