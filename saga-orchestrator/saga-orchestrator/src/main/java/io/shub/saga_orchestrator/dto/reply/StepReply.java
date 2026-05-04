package io.shub.saga_orchestrator.dto.reply;

import java.util.Map;

/**
 * Universal reply record consumed from all {@code *.replies} topics.
 * Every step service produces this same shape so the orchestrator
 * can handle all replies uniformly.
 *
 * @param sagaId         the saga instance this reply belongs to
 * @param stepName       identifier of the step (e.g., "RESERVE_INVENTORY")
 * @param status         "SUCCESS" or "FAILURE"
 * @param idempotencyKey the key that was sent in the original command
 * @param data           step-specific output data (e.g., reservationId, txnId)
 * @param errorMessage   null on success; reason on failure
 */
public record StepReply(
        String sagaId,
        String stepName,
        String status,
        String idempotencyKey,
        Map<String, Object> data,
        String errorMessage
) {
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
