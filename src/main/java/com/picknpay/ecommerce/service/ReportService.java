package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.response.OrderSummaryResponse;
import com.picknpay.ecommerce.dto.response.TopProductResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    List<TopProductResponse> topProducts(int limit, LocalDate from, LocalDate to);

    List<OrderSummaryResponse> orderSummary();
}
