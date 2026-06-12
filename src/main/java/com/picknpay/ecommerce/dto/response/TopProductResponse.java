package com.picknpay.ecommerce.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.Locale;

@Value
@Builder
public class TopProductResponse {
    Long productId;
    String productSku;
    String productName;
    Long totalUnitsSold;
    Long orderCount;
    Long totalRevenue;
    String totalRevenueFormatted;

    public static TopProductResponse of(Long productId, String sku, String name,
                                        Long units, Long orderCount, Long revenue) {
        return TopProductResponse.builder()
                .productId(productId)
                .productSku(sku)
                .productName(name)
                .totalUnitsSold(units)
                .orderCount(orderCount)
                .totalRevenue(revenue)
                .totalRevenueFormatted(String.format(Locale.UK, "R%.2f", revenue / 100.0))
                .build();
    }
}
