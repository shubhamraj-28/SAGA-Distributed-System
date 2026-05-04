package io.shub.inventory_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a stock reservation made during a saga step.
 * Maps to the {@code inventory.inventory_reservation} table.
 *
 * <p>Idempotency is enforced via a unique constraint on {@code idempotencyKey}.
 * Status transitions: RESERVED → RELEASED (on compensation).</p>
 */
@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA requires a no-arg constructor. */
    protected InventoryReservation() {}

    public InventoryReservation(UUID reservationId, Product product, int quantity,
                                 String status, UUID idempotencyKey) {
        this.reservationId = reservationId;
        this.product = product;
        this.quantity = quantity;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters ──

    public Long getId() {
        return id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Setters ──

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "InventoryReservation{reservationId=%s, productId=%d, qty=%d, status='%s'}"
                .formatted(reservationId, product != null ? product.getId() : null, quantity, status);
    }
}
