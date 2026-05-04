package io.shub.saga_orchestrator.repository;

import io.shub.saga_orchestrator.entity.SagaInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link SagaInstance} entities.
 */
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    /**
     * Find sagas that are stuck in RUNNING or COMPENSATING state
     * and haven't been updated since the given cutoff time.
     * Used by the timeout scheduler to detect and compensate timed-out sagas.
     */
    List<SagaInstance> findByStatusInAndUpdatedAtBefore(List<String> statuses, LocalDateTime cutoff);

    /**
     * Find all sagas with a specific status.
     */
    List<SagaInstance> findByStatus(String status);
}
