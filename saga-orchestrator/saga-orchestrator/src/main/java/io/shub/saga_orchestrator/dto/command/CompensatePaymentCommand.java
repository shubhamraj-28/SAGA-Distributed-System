package io.shub.saga_orchestrator.dto.command;

/**
 * Command published to {@code payment.compensate} topic.
 * Instructs payment-service to refund a previously charged transaction.
 */
public record CompensatePaymentCommand(
        String sagaId,
        String idempotencyKey,
        String txnId
) {}
