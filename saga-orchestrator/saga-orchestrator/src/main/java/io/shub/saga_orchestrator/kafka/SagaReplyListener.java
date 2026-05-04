package io.shub.saga_orchestrator.kafka;

import io.shub.saga_orchestrator.config.KafkaTopics;
import io.shub.saga_orchestrator.dto.reply.StepReply;
import io.shub.saga_orchestrator.engine.SagaEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes reply messages from ALL step service reply topics.
 *
 * <p>All reply topics produce the same {@link StepReply} JSON shape,
 * so a single listener handles all of them. The saga engine uses
 * the {@code stepName} field to identify which step replied.</p>
 */
@Component
public class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final SagaEngine sagaEngine;

    public SagaReplyListener(SagaEngine sagaEngine) {
        this.sagaEngine = sagaEngine;
    }

    /**
     * Listen to all reply topics and delegate to the saga engine.
     */
    @KafkaListener(
            topics = {
                KafkaTopics.INVENTORY_REPLIES,
                KafkaTopics.PAYMENT_REPLIES,
                KafkaTopics.NOTIFICATION_REPLIES
            },
            groupId = "saga-orchestrator-group",
            properties = {
                "spring.json.value.default.type=io.shub.saga_orchestrator.dto.reply.StepReply"
            }
    )
    public void handleReply(StepReply reply) {
        log.info("Received reply: sagaId={}, step={}, status={}",
                reply.sagaId(), reply.stepName(), reply.status());

        try {
            sagaEngine.handleReply(reply);
        } catch (Exception e) {
            log.error("Error handling reply: sagaId={}, step={}",
                    reply.sagaId(), reply.stepName(), e);
            // The timeout scheduler will catch unprocessed sagas
        }
    }
}
