package io.shub.saga_orchestrator.dto.response;

import java.time.LocalDateTime;

/**
 * REST response representing the current state of a saga instance.
 */
public record SagaResponse(
        String sagaId,
        String sagaType,
        String status,
        int currentStep,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
