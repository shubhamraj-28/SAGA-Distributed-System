package io.shub.inventory_service.dto;

/**
 * Command consumed from {@code inventory.commands} topic.
 * Instructs this service to reserve stock for the given SKU.
 */
public record ReserveInventoryCommand(
        String sagaId,
        String idempotencyKey,
        String sku,
        int quantity
) {}
