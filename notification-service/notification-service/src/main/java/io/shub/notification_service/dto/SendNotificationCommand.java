package io.shub.notification_service.dto;

/**
 * Command consumed from {@code notification.commands} topic.
 * Instructs this service to send a notification (email).
 * This step has NO compensation — can't un-send an email.
 */
public record SendNotificationCommand(
        String sagaId,
        String idempotencyKey,
        String userId,
        String email,
        String subject,
        String message
) {}
