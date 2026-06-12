package com.picknpay.ecommerce.repository;

import com.picknpay.ecommerce.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for ProductRepository. The DataJpaTest slice loads only JPA beans;
 * here Hibernate generates the schema from entities (ddl-auto=create-drop) so
 * the test does not depend on schema.sql or data.sql.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class ProductRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void seed() {
        persist("PnP Free Range Eggs", 3999, 50, "SKU-EGGS");
        persist("Clover Full Cream Milk", 2999, 30, "SKU-MILK");
        persist("Albany White Bread", 1999, 100, "SKU-BREAD");
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("search with no filters returns all products")
    void search_noFilters_returnsAll() {
        Page<Product> page = productRepository.search(null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("search by name is a case-insensitive substring match")
    void search_byName_caseInsensitive() {
        Page<Product> page = productRepository.search("eggs", null, null, PageRequest.of(0, 20));
        assertThat(page.getContent())
                .extracting(Product::getSku)
                .containsExactly("SKU-EGGS");
    }

    @Test
    @DisplayName("search by name returns empty when nothing matches")
    void search_byName_noMatch() {
        Page<Product> page = productRepository.search("xyz-no-match", null, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("search minPrice excludes cheaper products")
    void search_minPrice() {
        Page<Product> page = productRepository.search(null, 3000, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSku()).isEqualTo("SKU-EGGS");
    }

    @Test
    @DisplayName("search maxPrice excludes more expensive products")
    void search_maxPrice() {
        Page<Product> page = productRepository.search(null, null, 2999, PageRequest.of(0, 20));
        assertThat(page.getContent())
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-MILK", "SKU-BREAD");
    }

    @Test
    @DisplayName("search combined name + price range applies both predicates")
    void search_nameAndPrice() {
        Page<Product> page = productRepository.search("milk", null, 3000, PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSku()).isEqualTo("SKU-MILK");
    }

    @Test
    @DisplayName("search respects page size and returns the correct slice")
    void search_pagination() {
        PageRequest first  = PageRequest.of(0, 2, Sort.by("price").ascending());
        PageRequest second = PageRequest.of(1, 2, Sort.by("price").ascending());

        Page<Product> p0 = productRepository.search(null, null, null, first);
        Page<Product> p1 = productRepository.search(null, null, null, second);

        assertThat(p0.getContent()).hasSize(2);
        assertThat(p1.getContent()).hasSize(1);
        assertThat(p0.getTotalPages()).isEqualTo(2);
        assertThat(p0.getContent().get(0).getSku()).isEqualTo("SKU-BREAD"); // cheapest first
        assertThat(p0.getContent().get(1).getSku()).isEqualTo("SKU-MILK");
    }

    @Test
    @DisplayName("decrementStock reduces stock by the requested amount")
    void decrementStock() {
        Product eggs = findBySku("SKU-EGGS");
        int before = eggs.getStockQuantity();
        eggs.decrementStock(5);
        assertThat(eggs.getStockQuantity()).isEqualTo(before - 5);
    }

    @Test
    @DisplayName("restoreStock increases stock by the given amount")
    void restoreStock() {
        Product bread = findBySku("SKU-BREAD");
        int before = bread.getStockQuantity();
        bread.restoreStock(10);
        assertThat(bread.getStockQuantity()).isEqualTo(before + 10);
    }

    @Test
    @DisplayName("existsBySku returns true for an existing SKU")
    void existsBySku_true() {
        assertThat(productRepository.existsBySku("SKU-EGGS")).isTrue();
    }

    @Test
    @DisplayName("existsBySku returns false for an unknown SKU")
    void existsBySku_false() {
        assertThat(productRepository.existsBySku("SKU-DOES-NOT-EXIST")).isFalse();
    }

    private Product findBySku(String sku) {
        return productRepository.findAll().stream()
                .filter(p -> p.getSku().equals(sku)).findFirst().orElseThrow();
    }

    private void persist(String name, int price, int stock, String sku) {
        em.persist(Product.builder()
                .name(name).price(price).stockQuantity(stock).sku(sku).build());
    }
}
