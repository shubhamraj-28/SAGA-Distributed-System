package io.shub.inventory_service.dto;

/**
 * Compensation command consumed from {@code inventory.compensate} topic.
 * Instructs this service to release a previously made reservation.
 */
public record CompensateInventoryCommand(
        String sagaId,
        String idempotencyKey,
        String reservationId
) {}
