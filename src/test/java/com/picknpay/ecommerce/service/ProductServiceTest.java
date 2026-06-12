package com.picknpay.ecommerce.service;

import com.picknpay.ecommerce.dto.request.CreateProductRequest;
import com.picknpay.ecommerce.dto.response.ProductResponse;
import com.picknpay.ecommerce.exception.DuplicateSkuException;
import com.picknpay.ecommerce.exception.ResourceNotFoundException;
import com.picknpay.ecommerce.model.Product;
import com.picknpay.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock  private ProductRepository productRepository;
    @InjectMocks private ProductServiceImpl productService;

    private Product sample;

    @BeforeEach
    void setUp() {
        sample = Product.builder()
                .id(1L).name("Test").description("d")
                .price(4999).stockQuantity(100).sku("SKU-TEST")
                .build();
    }

    @Test
    @DisplayName("createProduct saves and returns mapped response")
    void create_happy() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Test").price(4999).stockQuantity(100).sku("SKU-TEST").build();
        when(productRepository.existsBySku("SKU-TEST")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sample);

        ProductResponse out = productService.createProduct(req);

        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getPriceFormatted()).isEqualTo("R49.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct throws DuplicateSkuException when SKU already exists")
    void create_duplicateSku() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Test").price(1).stockQuantity(1).sku("SKU-TEST").build();
        when(productRepository.existsBySku("SKU-TEST")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(req))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-TEST");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProducts delegates to search with all four params")
    void getProducts_passesFilters() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(sample), pageable, 1);
        when(productRepository.search("eggs", 100, 5000, pageable)).thenReturn(page);

        Page<ProductResponse> out = productService.getProducts("eggs", 100, 5000, pageable);

        assertThat(out.getTotalElements()).isEqualTo(1);
        assertThat(out.getContent().get(0).getSku()).isEqualTo("SKU-TEST");
    }

    @Test
    @DisplayName("getProductById returns mapped response when product exists")
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sample));

        ProductResponse out = productService.getProductById(1L);

        assertThat(out.getId()).isEqualTo(1L);
        assertThat(out.getStockQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("getProductById throws ResourceNotFoundException for unknown ID")
    void getProductById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
    }
}
