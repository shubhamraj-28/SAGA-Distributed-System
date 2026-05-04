package io.shub.inventory_service.repository;

import io.shub.inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for {@link Product} entities.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its unique SKU code.
     */
    Optional<Product> findBySku(String sku);
}
