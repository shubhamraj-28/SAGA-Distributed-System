package io.shub.notification_service.dto;

import java.util.Map;

/**
 * Reply produced to {@code notification.replies} topic.
 * Follows the universal reply contract expected by the orchestrator.
 */
public record StepReply(
        String sagaId,
        String stepName,
        String status,
        String idempotencyKey,
        Map<String, Object> data,
        String errorMessage
) {
    /** Factory for a successful reply. */
    public static StepReply success(String sagaId, String idempotencyKey) {
        return new StepReply(sagaId, "SEND_NOTIFICATION", "SUCCESS", idempotencyKey, null, null);
    }

    /** Factory for a failed reply. */
    public static StepReply failure(String sagaId, String idempotencyKey, String errorMessage) {
        return new StepReply(sagaId, "SEND_NOTIFICATION", "FAILURE", idempotencyKey, null, errorMessage);
    }
}
