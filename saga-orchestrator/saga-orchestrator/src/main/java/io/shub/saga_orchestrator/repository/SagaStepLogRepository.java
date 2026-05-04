package io.shub.saga_orchestrator.repository;

import io.shub.saga_orchestrator.entity.SagaStepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link SagaStepLog} entities.
 */
public interface SagaStepLogRepository extends JpaRepository<SagaStepLog, Long> {

    /**
     * Find all step logs for a given saga, ordered by creation time.
     */
    List<SagaStepLog> findBySagaInstanceIdOrderByCreatedAtAsc(UUID sagaId);

    /**
     * Find the SUCCESS reply log for a specific step index.
     * Used during compensation to extract step output data (reservationId, txnId, etc.).
     */
    List<SagaStepLog> findBySagaInstanceIdAndStepIndexAndActionTypeAndStatus(
            UUID sagaId, int stepIndex, String actionType, String status);
}
