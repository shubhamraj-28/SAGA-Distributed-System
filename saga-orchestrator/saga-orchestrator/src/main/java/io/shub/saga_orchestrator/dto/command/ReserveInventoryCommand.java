package io.shub.saga_orchestrator.dto.command;

/**
 * Command published to {@code inventory.commands} topic.
 * Instructs inventory-service to reserve stock for a given SKU.
 */
public record ReserveInventoryCommand(
        String sagaId,
        String idempotencyKey,
        String sku,
        int quantity
) {}
