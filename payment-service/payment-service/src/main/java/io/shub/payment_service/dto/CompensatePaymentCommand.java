package io.shub.payment_service.dto;

/**
 * Compensation command consumed from {@code payment.compensate} topic.
 * Instructs this service to refund a previously charged transaction.
 */
public record CompensatePaymentCommand(
        String sagaId,
        String idempotencyKey,
        String txnId
) {}
