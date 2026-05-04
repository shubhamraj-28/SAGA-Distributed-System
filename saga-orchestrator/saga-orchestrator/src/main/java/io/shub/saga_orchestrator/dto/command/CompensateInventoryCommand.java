package io.shub.saga_orchestrator.dto.command;

/**
 * Command published to {@code inventory.compensate} topic.
 * Instructs inventory-service to release a previously made reservation.
 */
public record CompensateInventoryCommand(
        String sagaId,
        String idempotencyKey,
        String reservationId
) {}
