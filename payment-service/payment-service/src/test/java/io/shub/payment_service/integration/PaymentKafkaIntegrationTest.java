package io.shub.payment_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.shub.payment_service.dto.ChargePaymentCommand;
import io.shub.payment_service.kafka.KafkaTopics;
import io.shub.payment_service.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Kafka + PostgreSQL from {@code docker compose up -d}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
        properties = {
            "spring.jpa.show-sql=false",
            "spring.datasource.url=jdbc:postgresql://localhost:5434/saga_distributed?currentSchema=payment&options=-c%20TimeZone=UTC",
            "spring.datasource.username=saga_admin",
            "spring.datasource.password=saga_secret",
            "spring.kafka.bootstrap-servers=localhost:9092"
        })
class PaymentKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Test
    void chargeCommand_persistsTransaction() {
        UUID sagaId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        long before = transactionRepository.count();

        ChargePaymentCommand command = new ChargePaymentCommand(
                sagaId.toString(), idempotencyKey.toString(), "user-it", new BigDecimal("42.50"));
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, sagaId.toString(), command);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(transactionRepository.count())
                .isEqualTo(before + 1));
    }
}
