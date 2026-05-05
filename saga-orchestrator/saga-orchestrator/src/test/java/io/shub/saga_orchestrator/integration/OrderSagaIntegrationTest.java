package io.shub.saga_orchestrator.integration;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shub.saga_orchestrator.dto.request.OrderItem;
import io.shub.saga_orchestrator.dto.request.StartSagaRequest;
import io.shub.saga_orchestrator.dto.response.SagaResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full ORDER_SAGA against PostgreSQL + Kafka from {@code docker compose up -d} (localhost:5434, 9092).
 * Stub participants reply on command topics so worker apps do not need to run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(IntegrationStubParticipantsConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.jpa.show-sql=false",
            "spring.test.database.replace=none",
            "spring.datasource.url=jdbc:postgresql://localhost:5434/saga_distributed?currentSchema=orchestrator&options=-c%20TimeZone=UTC",
            "spring.datasource.username=saga_admin",
            "spring.datasource.password=saga_secret",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
            "spring.kafka.bootstrap-servers=localhost:9092",
            "saga.timeout.check-interval-ms=600000",
            "saga.timeout.step-timeout-seconds=600"
        })
class OrderSagaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startSaga_orderFlowCompletes() throws Exception {
        StartSagaRequest body = new StartSagaRequest(
                "user-it",
                "user-it@example.com",
                List.of(new OrderItem("SKU-MOUSE-001", 1, new BigDecimal("29.99"))));

        MvcResult created = mockMvc.perform(post("/api/sagas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        SagaResponse sagaResponse =
                objectMapper.readValue(created.getResponse().getContentAsString(), SagaResponse.class);
        UUID sagaId = UUID.fromString(sagaResponse.sagaId());

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> mockMvc.perform(get("/api/sagas/" + sagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED")));
    }
}
