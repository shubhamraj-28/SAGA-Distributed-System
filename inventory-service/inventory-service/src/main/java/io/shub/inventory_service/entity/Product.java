package io.shub.inventory_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a product in the inventory catalog.
 * Maps to the {@code inventory.product} table.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    /** JPA requires a no-arg constructor. */
    protected Product() {}

    public Product(String name, String sku, int availableQty) {
        this.name = name;
        this.sku = sku;
        this.availableQty = availableQty;
    }

    // ── Getters ──

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    // ── Setters ──

    public void setAvailableQty(int availableQty) {
        this.availableQty = availableQty;
    }

    @Override
    public String toString() {
        return "Product{id=%d, sku='%s', availableQty=%d}".formatted(id, sku, availableQty);
    }
}
