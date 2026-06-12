package com.picknpay.ecommerce.controller;

import com.picknpay.ecommerce.dto.request.CreateProductRequest;
import com.picknpay.ecommerce.dto.response.ProductResponse;
import com.picknpay.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalogue. GET endpoints are public; POST requires Basic Auth.")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request,
                                                  UriComponentsBuilder uri) {
        ProductResponse created = productService.createProduct(request);
        URI location = uri.path("/api/v1/products/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List products with optional filters and pagination")
    public Page<ProductResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            Pageable pageable) {
        return productService.getProducts(name, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product by ID")
    public ProductResponse getOne(@PathVariable Long id) {
        return productService.getProductById(id);
    }
}
