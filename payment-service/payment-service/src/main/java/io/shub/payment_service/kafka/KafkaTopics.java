package io.shub.payment_service.kafka;

/**
 * Kafka topic constants for the payment service.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String PAYMENT_COMMANDS     = "payment.commands";
    public static final String PAYMENT_REPLIES      = "payment.replies";
    public static final String PAYMENT_COMPENSATE   = "payment.compensate";
}
