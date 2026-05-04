package io.shub.saga_orchestrator.model;

/**
 * Defines a single step within a saga.
 *
 * @param stepName        unique identifier for this step (e.g., "RESERVE_INVENTORY")
 * @param commandTopic    Kafka topic to publish the command to
 * @param replyTopic      Kafka topic to consume the reply from
 * @param compensateTopic Kafka topic to publish the compensate command to (null if non-compensable)
 */
public record SagaStep(
        String stepName,
        String commandTopic,
        String replyTopic,
        String compensateTopic
) {
    /**
     * Whether this step supports compensation.
     * Steps without compensation (e.g., notification) should be placed
     * at the end of the saga so they don't block rollback.
     */
    public boolean isCompensable() {
        return compensateTopic != null;
    }
}
