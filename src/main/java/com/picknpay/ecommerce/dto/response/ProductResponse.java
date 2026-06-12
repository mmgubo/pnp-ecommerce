package com.picknpay.ecommerce.dto.response;

import com.picknpay.ecommerce.model.Product;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Locale;

@Value
@Builder
public class ProductResponse {
    Long id;
    String name;
    String description;
    /** Raw price in cents — e.g. 3999. */
    Integer price;
    /** Human-formatted price — e.g. "R39.99". */
    String priceFormatted;
    Integer stockQuantity;
    String sku;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .priceFormatted(formatRand(p.getPrice()))
                .stockQuantity(p.getStockQuantity())
                .sku(p.getSku())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private static String formatRand(Integer cents) {
        if (cents == null) return null;
        return String.format(Locale.UK, "R%.2f", cents / 100.0);
    }
}
