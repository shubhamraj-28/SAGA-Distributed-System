package io.shub.notification_service.kafka;

/**
 * Kafka topic constants for the notification service.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String NOTIFICATION_COMMANDS = "notification.commands";
    public static final String NOTIFICATION_REPLIES  = "notification.replies";
    // No compensation topic — can't un-send an email
}
