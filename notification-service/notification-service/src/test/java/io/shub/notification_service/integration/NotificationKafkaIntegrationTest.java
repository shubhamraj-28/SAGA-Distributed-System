package io.shub.notification_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.shub.notification_service.dto.SendNotificationCommand;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "notification.simulate-failure=false")
@EmbeddedKafka(
        partitions = 1,
        topics = {"notification.commands", "notification.replies"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void notificationCommand_producesSuccessReply() {
        String sagaId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        SendNotificationCommand command =
                new SendNotificationCommand(sagaId, idempotencyKey, "user-it", "user-it@example.com", "Hi", "Body");

        kafkaTemplate.send("notification.commands", sagaId, command);

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils.consumerProps(
                "notification-it-group", "true", embeddedKafkaBroker));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            consumer.subscribe(List.of("notification.replies"));
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, Duration.ofSeconds(20));
            assertThat(record.value()).contains("SUCCESS").contains("SEND_NOTIFICATION");
        }
    }
}
