package io.shub.saga_orchestrator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists the lifecycle of a single saga execution.
 * Maps to the {@code orchestrator.saga_instance} table.
 *
 * <p>Optimistic locking is enforced via the {@code version} field
 * ({@link Version}) to prevent concurrent updates from corrupting
 * the saga state machine.</p>
 *
 * <p>Status transitions:
 * <pre>
 *   STARTED → RUNNING → COMPLETED
 *                     ↘ COMPENSATING → FAILED
 * </pre></p>
 */
@Entity
@Table(name = "saga_instance")
public class SagaInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private int version;

    /** JPA requires a no-arg constructor. */
    protected SagaInstance() {}

    public SagaInstance(String sagaType, String payload) {
        this.sagaType = sagaType;
        this.payload = payload;
        this.currentStep = 0;
        this.status = "STARTED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters ──

    public UUID getId() {
        return id;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getPayload() {
        return payload;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    // ── Setters ──

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SagaInstance{id=%s, type='%s', step=%d, status='%s', version=%d}"
                .formatted(id, sagaType, currentStep, status, version);
    }
}
