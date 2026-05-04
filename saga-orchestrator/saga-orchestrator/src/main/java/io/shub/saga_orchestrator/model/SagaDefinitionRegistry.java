package io.shub.saga_orchestrator.model;

import io.shub.saga_orchestrator.config.KafkaTopics;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all known saga definitions.
 * Add new saga types here as the system grows.
 */
@Component
public class SagaDefinitionRegistry {

    private final Map<String, SagaDefinition> registry = new HashMap<>();

    public SagaDefinitionRegistry() {
        registerOrderSaga();
    }

    /**
     * Look up a saga definition by its type name.
     */
    public Optional<SagaDefinition> findByType(String sagaType) {
        return Optional.ofNullable(registry.get(sagaType));
    }

    /**
     * ORDER_SAGA: Reserve inventory → Charge payment → Send notification
     *
     * <p>Notification is intentionally last because it has no compensation.
     * If notification fails, only payment and inventory are rolled back.</p>
     */
    private void registerOrderSaga() {
        SagaDefinition orderSaga = new SagaDefinition("ORDER_SAGA", List.of(
                new SagaStep(
                        "RESERVE_INVENTORY",
                        KafkaTopics.INVENTORY_COMMANDS,
                        KafkaTopics.INVENTORY_REPLIES,
                        KafkaTopics.INVENTORY_COMPENSATE
                ),
                new SagaStep(
                        "CHARGE_PAYMENT",
                        KafkaTopics.PAYMENT_COMMANDS,
                        KafkaTopics.PAYMENT_REPLIES,
                        KafkaTopics.PAYMENT_COMPENSATE
                ),
                new SagaStep(
                        "SEND_NOTIFICATION",
                        KafkaTopics.NOTIFICATION_COMMANDS,
                        KafkaTopics.NOTIFICATION_REPLIES,
                        null  // No compensation — can't un-send an email
                )
        ));

        registry.put(orderSaga.sagaType(), orderSaga);
    }
}
