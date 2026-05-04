package io.shub.saga_orchestrator.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes saga commands and compensation messages to Kafka topics.
 * Uses the sagaId as the Kafka message key to ensure all messages
 * for a saga land on the same partition (ordering guarantee).
 */
@Component
public class SagaCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SagaCommandPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish a command to a Kafka topic.
     *
     * @param topic   the target Kafka topic
     * @param sagaId  used as the message key (partition affinity)
     * @param command the command payload (will be serialized to JSON)
     */
    public void publish(String topic, String sagaId, Object command) {
        log.info("Publishing to topic={}, sagaId={}, command={}", topic, sagaId, command.getClass().getSimpleName());
        kafkaTemplate.send(topic, sagaId, command);
    }
}
