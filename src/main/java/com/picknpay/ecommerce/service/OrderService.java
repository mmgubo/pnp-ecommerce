package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.request.PlaceOrderRequest;
import com.picknpay.ecommerce.dto.response.OrderResponse;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse cancelOrder(Long id);
}
