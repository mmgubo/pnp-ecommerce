package com.picknpay.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    /** Price in cents; e.g. R39.99 = 3999. Stored as INT to avoid float rounding. */
    @Column(nullable = false)
    private Integer price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void decrementStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("decrement quantity must be positive");
        }
        if (stockQuantity < qty) {
            throw new IllegalStateException("insufficient stock for product " + sku);
        }
        stockQuantity -= qty;
    }

    public void restoreStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("restore quantity must be positive");
        }
        stockQuantity += qty;
    }
}
