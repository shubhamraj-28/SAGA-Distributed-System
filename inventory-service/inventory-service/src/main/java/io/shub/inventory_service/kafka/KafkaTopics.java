package io.shub.inventory_service.kafka;

/**
 * Kafka topic constants for the inventory service.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String INVENTORY_COMMANDS   = "inventory.commands";
    public static final String INVENTORY_REPLIES     = "inventory.replies";
    public static final String INVENTORY_COMPENSATE  = "inventory.compensate";
}
