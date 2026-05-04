package io.shub.saga_orchestrator.dto.command;

import java.math.BigDecimal;

/**
 * Command published to {@code payment.commands} topic.
 * Instructs payment-service to charge the user.
 */
public record ChargePaymentCommand(
        String sagaId,
        String idempotencyKey,
        String userId,
        BigDecimal amount
) {}
