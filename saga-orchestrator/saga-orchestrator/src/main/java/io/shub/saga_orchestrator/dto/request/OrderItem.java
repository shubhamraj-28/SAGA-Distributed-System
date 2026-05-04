package io.shub.saga_orchestrator.dto.request;

import java.math.BigDecimal;

/**
 * Represents a single item in an order.
 */
public record OrderItem(
        String sku,
        int quantity,
        BigDecimal price
) {}
