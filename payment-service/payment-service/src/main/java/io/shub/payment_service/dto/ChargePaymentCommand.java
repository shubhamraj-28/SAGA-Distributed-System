package io.shub.payment_service.dto;

import java.math.BigDecimal;

/**
 * Command consumed from {@code payment.commands} topic.
 * Instructs this service to charge the user.
 */
public record ChargePaymentCommand(
        String sagaId,
        String idempotencyKey,
        String userId,
        BigDecimal amount
) {}
