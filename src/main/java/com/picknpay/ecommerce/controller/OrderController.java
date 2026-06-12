package com.picknpay.ecommerce.controller;

import com.picknpay.ecommerce.dto.request.PlaceOrderRequest;
import com.picknpay.ecommerce.dto.response.OrderResponse;
import com.picknpay.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place, retrieve, and cancel customer orders. POST and PATCH require Basic Auth.")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order (decrements product stock atomically)")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request,
                                               UriComponentsBuilder uri) {
        OrderResponse created = orderService.placeOrder(request);
        URI location = uri.path("/api/v1/orders/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a single order with its items")
    public OrderResponse getOne(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a CONFIRMED order and restore the reserved stock")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}
