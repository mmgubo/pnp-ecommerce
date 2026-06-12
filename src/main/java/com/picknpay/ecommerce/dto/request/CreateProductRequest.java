package com.picknpay.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be 255 characters or fewer")
    private String name;

    @Size(max = 4000, message = "description must be 4000 characters or fewer")
    private String description;

    @NotNull(message = "price is required")
    @Min(value = 0, message = "price must be zero or greater (in cents)")
    private Integer price;

    @NotNull(message = "stockQuantity is required")
    @Min(value = 0, message = "stockQuantity must be zero or greater")
    private Integer stockQuantity;

    @NotBlank(message = "sku is required")
    @Size(max = 100, message = "sku must be 100 characters or fewer")
    private String sku;
}
