package com.picknpay.ecommerce.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.Locale;

@Value
@Builder
public class OrderSummaryResponse {
    String status;
    Long orderCount;
    Long totalRevenue;
    String totalRevenueFormatted;
    Long averageOrderValue;
    String averageOrderValueFormatted;

    public static OrderSummaryResponse of(String status, Long count, Long total, Long avg) {
        return OrderSummaryResponse.builder()
                .status(status)
                .orderCount(count)
                .totalRevenue(total)
                .totalRevenueFormatted(String.format(Locale.UK, "R%.2f", total / 100.0))
                .averageOrderValue(avg)
                .averageOrderValueFormatted(String.format(Locale.UK, "R%.2f", avg / 100.0))
                .build();
    }
}
