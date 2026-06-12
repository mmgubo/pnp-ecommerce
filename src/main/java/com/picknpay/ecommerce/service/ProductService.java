package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.request.CreateProductRequest;
import com.picknpay.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    Page<ProductResponse> getProducts(String name, Integer minPrice, Integer maxPrice, Pageable pageable);

    ProductResponse getProductById(Long id);
}
