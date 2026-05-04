package io.shub.saga_orchestrator.dto.command;

/**
 * Command published to {@code notification.commands} topic.
 * Instructs notification-service to send an email/notification.
 * This step has NO compensation (can't un-send an email).
 */
public record SendNotificationCommand(
        String sagaId,
        String idempotencyKey,
        String userId,
        String email,
        String subject,
        String message
) {}
