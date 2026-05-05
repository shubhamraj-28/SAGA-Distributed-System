package io.shub.saga_orchestrator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entry for every action taken on a saga step.
 * Maps to the {@code orchestrator.saga_step_log} table.
 *
 * <p>Action types: COMMAND (step dispatched), REPLY (reply received), COMPENSATE (compensation sent).
 * Statuses: SENT, SUCCESS, FAILURE.</p>
 */
@Entity
@Table(name = "saga_step_log")
public class SagaStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaInstance sagaInstance;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** JPA requires a no-arg constructor. */
    protected SagaStepLog() {}

    public SagaStepLog(SagaInstance sagaInstance, int stepIndex, String stepName,
                       String actionType, String status, String payload, UUID idempotencyKey) {
        this.sagaInstance = sagaInstance;
        this.stepIndex = stepIndex;
        this.stepName = stepName;
        this.actionType = actionType;
        this.status = status;
        this.payload = payload;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters ──

    public Long getId() {
        return id;
    }

    public SagaInstance getSagaInstance() {
        return sagaInstance;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public String getStepName() {
        return stepName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "SagaStepLog{sagaId=%s, step=%d, name='%s', action='%s', status='%s'}"
                .formatted(sagaInstance != null ? sagaInstance.getId() : null,
                        stepIndex, stepName, actionType, status);
    }
}
