package com.picknpay.ecommerce.repository;

import com.picknpay.ecommerce.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Paginated catalogue search. Every filter is optional; passing null skips that predicate.
     * Name match is case-insensitive substring; price bounds are inclusive (in cents).
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:name     IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> search(
            @Param("name")     String  name,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable);

    /**
     * Acquires a pessimistic write lock (SELECT ... FOR UPDATE) on the row before stock is mutated.
     * Used by the order service to prevent concurrent over-selling.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    boolean existsBySku(String sku);
}
