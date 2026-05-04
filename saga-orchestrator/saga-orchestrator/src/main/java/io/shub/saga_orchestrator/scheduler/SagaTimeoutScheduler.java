package io.shub.saga_orchestrator.scheduler;

import io.shub.saga_orchestrator.engine.SagaEngine;
import io.shub.saga_orchestrator.entity.SagaInstance;
import io.shub.saga_orchestrator.model.SagaDefinition;
import io.shub.saga_orchestrator.model.SagaDefinitionRegistry;
import io.shub.saga_orchestrator.repository.SagaInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically scans for timed-out sagas and triggers compensation.
 *
 * <p>A saga is considered timed-out if it has been in RUNNING or COMPENSATING
 * status for longer than the configured timeout period. This catches scenarios
 * where a step service crashes without sending a reply.</p>
 */
@Component
public class SagaTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutScheduler.class);

    private final SagaInstanceRepository sagaRepository;
    private final SagaEngine sagaEngine;
    private final SagaDefinitionRegistry definitionRegistry;
    private final long stepTimeoutSeconds;

    public SagaTimeoutScheduler(SagaInstanceRepository sagaRepository,
                                 SagaEngine sagaEngine,
                                 SagaDefinitionRegistry definitionRegistry,
                                 @Value("${saga.timeout.step-timeout-seconds:120}") long stepTimeoutSeconds) {
        this.sagaRepository = sagaRepository;
        this.sagaEngine = sagaEngine;
        this.definitionRegistry = definitionRegistry;
        this.stepTimeoutSeconds = stepTimeoutSeconds;
    }

    /**
     * Runs periodically to detect sagas that have been stuck for too long.
     * The interval is configured via {@code saga.timeout.check-interval-ms}.
     */
    @Scheduled(fixedDelayString = "${saga.timeout.check-interval-ms:30000}")
    public void detectTimedOutSagas() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(stepTimeoutSeconds);

        List<SagaInstance> stuckSagas = sagaRepository.findByStatusInAndUpdatedAtBefore(
                List.of("RUNNING", "COMPENSATING"), cutoff);

        if (stuckSagas.isEmpty()) {
            return;
        }

        log.warn("Found {} timed-out saga(s), triggering compensation", stuckSagas.size());

        for (SagaInstance saga : stuckSagas) {
            try {
                log.warn("Compensating timed-out saga: id={}, status={}, lastUpdated={}",
                        saga.getId(), saga.getStatus(), saga.getUpdatedAt());

                SagaDefinition definition = definitionRegistry.findByType(saga.getSagaType())
                        .orElse(null);

                if (definition == null) {
                    log.error("No definition for saga type: {}", saga.getSagaType());
                    continue;
                }

                saga.setStatus("COMPENSATING");
                sagaRepository.save(saga);

                sagaEngine.compensateCompletedSteps(saga, definition);

                saga.setStatus("FAILED");
                sagaRepository.save(saga);

                log.info("Timed-out saga compensated: id={}", saga.getId());
            } catch (Exception e) {
                log.error("Error compensating timed-out saga: id={}", saga.getId(), e);
            }
        }
    }
}
