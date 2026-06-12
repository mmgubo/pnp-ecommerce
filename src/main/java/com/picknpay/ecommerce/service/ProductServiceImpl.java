package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.request.CreateProductRequest;
import com.picknpay.ecommerce.dto.response.ProductResponse;
import com.picknpay.ecommerce.exception.DuplicateSkuException;
import com.picknpay.ecommerce.exception.ResourceNotFoundException;
import com.picknpay.ecommerce.model.Product;
import com.picknpay.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product sku={}", request.getSku());
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Product saved = productRepository.save(Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .build());
        return ProductResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String name, Integer minPrice, Integer maxPrice, Pageable pageable) {
        return productRepository.search(name, minPrice, maxPrice, pageable).map(ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
