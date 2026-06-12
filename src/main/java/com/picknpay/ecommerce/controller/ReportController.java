package com.picknpay.ecommerce.controller;

import com.picknpay.ecommerce.dto.response.OrderSummaryResponse;
import com.picknpay.ecommerce.dto.response.TopProductResponse;
import com.picknpay.ecommerce.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reports", description = "Public read-only sales aggregates.")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/top-products")
    @Operation(summary = "Top N products by units sold (CONFIRMED orders only). Optional ISO date range.")
    public List<TopProductResponse> topProducts(
            @RequestParam(defaultValue = "10")
            @Min(value = 1,  message = "limit must be at least 1")
            @Max(value = 50, message = "limit must not exceed 50")
            int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.topProducts(limit, from, to);
    }

    @GetMapping("/order-summary")
    @Operation(summary = "Order counts, total revenue, and average order value grouped by status.")
    public List<OrderSummaryResponse> orderSummary() {
        return reportService.orderSummary();
    }
}
