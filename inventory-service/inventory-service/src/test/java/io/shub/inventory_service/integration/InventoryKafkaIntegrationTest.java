package io.shub.inventory_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.shub.inventory_service.dto.ReserveInventoryCommand;
import io.shub.inventory_service.kafka.KafkaTopics;
import io.shub.inventory_service.repository.ProductRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Kafka + PostgreSQL from {@code docker compose up -d} (same ports as {@code application.yml}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
        properties = {
            "spring.jpa.show-sql=false",
            "spring.datasource.url=jdbc:postgresql://localhost:5434/saga_distributed?currentSchema=inventory&options=-c%20TimeZone=UTC",
            "spring.datasource.username=saga_admin",
            "spring.datasource.password=saga_secret",
            "spring.kafka.bootstrap-servers=localhost:9092"
        })
class InventoryKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void reserveCommand_reducesAvailableQuantity() {
        UUID sagaId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        int before = productRepository.findBySku("SKU-MOUSE-001").orElseThrow().getAvailableQty();

        ReserveInventoryCommand command =
                new ReserveInventoryCommand(sagaId.toString(), idempotencyKey.toString(), "SKU-MOUSE-001", 2);
        kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, sagaId.toString(), command);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(
                        productRepository.findBySku("SKU-MOUSE-001").orElseThrow().getAvailableQty())
                .isEqualTo(before - 2));
    }
}
