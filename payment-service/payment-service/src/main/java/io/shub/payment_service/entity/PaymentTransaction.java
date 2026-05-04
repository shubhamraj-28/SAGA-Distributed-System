package io.shub.payment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a payment transaction created during a saga step.
 * Maps to the {@code payment.payment_transaction} table.
 *
 * <p>Idempotency is enforced via a unique constraint on {@code idempotencyKey}.
 * Status transitions: CHARGED → REFUNDED (on compensation).</p>
 */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", nullable = false, unique = true)
    private UUID txnId;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA requires a no-arg constructor. */
    protected PaymentTransaction() {}

    public PaymentTransaction(UUID txnId, UUID sagaId, String userId,
                               BigDecimal amount, String status, UUID idempotencyKey) {
        this.txnId = txnId;
        this.sagaId = sagaId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters ──

    public Long getId() {
        return id;
    }

    public UUID getTxnId() {
        return txnId;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
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
        return "PaymentTransaction{txnId=%s, userId='%s', amount=%s, status='%s'}"
                .formatted(txnId, userId, amount, status);
    }
}
