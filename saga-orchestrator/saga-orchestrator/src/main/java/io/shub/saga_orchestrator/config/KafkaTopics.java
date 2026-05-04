package io.shub.saga_orchestrator.config;

/**
 * Central registry of all Kafka topic names used in the saga system.
 * Keeps topic strings in one place to avoid typos and enable refactoring.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // ── Inventory (Step 0) ──
    public static final String INVENTORY_COMMANDS   = "inventory.commands";
    public static final String INVENTORY_REPLIES     = "inventory.replies";
    public static final String INVENTORY_COMPENSATE  = "inventory.compensate";

    // ── Payment (Step 1) ──
    public static final String PAYMENT_COMMANDS      = "payment.commands";
    public static final String PAYMENT_REPLIES       = "payment.replies";
    public static final String PAYMENT_COMPENSATE    = "payment.compensate";

    // ── Notification (Step 2) ──
    public static final String NOTIFICATION_COMMANDS = "notification.commands";
    public static final String NOTIFICATION_REPLIES  = "notification.replies";
    // No compensation topic — can't un-send an email
}
