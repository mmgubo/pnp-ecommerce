package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.response.OrderSummaryResponse;
import com.picknpay.ecommerce.dto.response.TopProductResponse;
import com.picknpay.ecommerce.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(int limit, LocalDate from, LocalDate to) {
        return reportRepository.topProducts(limit, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> orderSummary() {
        return reportRepository.orderSummary();
    }
}
