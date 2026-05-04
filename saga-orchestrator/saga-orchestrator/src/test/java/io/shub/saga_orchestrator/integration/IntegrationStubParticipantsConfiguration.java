package io.shub.saga_orchestrator.integration;

import io.shub.saga_orchestrator.config.KafkaTopics;
import io.shub.saga_orchestrator.dto.command.ChargePaymentCommand;
import io.shub.saga_orchestrator.dto.command.ReserveInventoryCommand;
import io.shub.saga_orchestrator.dto.command.SendNotificationCommand;
import io.shub.saga_orchestrator.dto.reply.StepReply;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test-only Kafka listeners that mimic inventory, payment, and notification services so the
 * orchestrator can complete ORDER_SAGA without starting the real worker applications.
 */
@TestConfiguration
public class IntegrationStubParticipantsConfiguration {

    @Bean
    StubParticipantListeners stubParticipantListeners(KafkaTemplate<String, Object> kafkaTemplate) {
        return new StubParticipantListeners(kafkaTemplate);
    }

    public static final class StubParticipantListeners {

        private final KafkaTemplate<String, Object> kafkaTemplate;

        StubParticipantListeners(KafkaTemplate<String, Object> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        @KafkaListener(
                topics = KafkaTopics.INVENTORY_COMMANDS,
                groupId = "stub-inventory-participants",
                properties = {
                    "spring.json.value.default.type=io.shub.saga_orchestrator.dto.command.ReserveInventoryCommand"
                })
        public void onInventoryCommand(ReserveInventoryCommand command) {
            StepReply reply = new StepReply(
                    command.sagaId(),
                    "RESERVE_INVENTORY",
                    "SUCCESS",
                    command.idempotencyKey(),
                    Map.of("reservationId", UUID.randomUUID().toString()),
                    null);
            kafkaTemplate.send(KafkaTopics.INVENTORY_REPLIES, command.sagaId(), reply);
        }

        @KafkaListener(
                topics = KafkaTopics.PAYMENT_COMMANDS,
                groupId = "stub-payment-participants",
                properties = {
                    "spring.json.value.default.type=io.shub.saga_orchestrator.dto.command.ChargePaymentCommand"
                })
        public void onPaymentCommand(ChargePaymentCommand command) {
            StepReply reply = new StepReply(
                    command.sagaId(),
                    "CHARGE_PAYMENT",
                    "SUCCESS",
                    command.idempotencyKey(),
                    Map.of("txnId", UUID.randomUUID().toString()),
                    null);
            kafkaTemplate.send(KafkaTopics.PAYMENT_REPLIES, command.sagaId(), reply);
        }

        @KafkaListener(
                topics = KafkaTopics.NOTIFICATION_COMMANDS,
                groupId = "stub-notification-participants",
                properties = {
                    "spring.json.value.default.type=io.shub.saga_orchestrator.dto.command.SendNotificationCommand"
                })
        public void onNotificationCommand(SendNotificationCommand command) {
            StepReply reply = new StepReply(
                    command.sagaId(),
                    "SEND_NOTIFICATION",
                    "SUCCESS",
                    command.idempotencyKey(),
                    null,
                    null);
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_REPLIES, command.sagaId(), reply);
        }
    }
}
