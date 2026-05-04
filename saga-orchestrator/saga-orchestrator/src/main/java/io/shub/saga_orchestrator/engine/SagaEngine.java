package io.shub.saga_orchestrator.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shub.saga_orchestrator.dto.command.ChargePaymentCommand;
import io.shub.saga_orchestrator.dto.command.CompensateInventoryCommand;
import io.shub.saga_orchestrator.dto.command.CompensatePaymentCommand;
import io.shub.saga_orchestrator.dto.command.ReserveInventoryCommand;
import io.shub.saga_orchestrator.dto.command.SendNotificationCommand;
import io.shub.saga_orchestrator.dto.reply.StepReply;
import io.shub.saga_orchestrator.dto.request.OrderItem;
import io.shub.saga_orchestrator.dto.request.StartSagaRequest;
import io.shub.saga_orchestrator.entity.SagaInstance;
import io.shub.saga_orchestrator.entity.SagaStepLog;
import io.shub.saga_orchestrator.kafka.SagaCommandPublisher;
import io.shub.saga_orchestrator.model.SagaDefinition;
import io.shub.saga_orchestrator.model.SagaDefinitionRegistry;
import io.shub.saga_orchestrator.model.SagaStep;
import io.shub.saga_orchestrator.repository.SagaInstanceRepository;
import io.shub.saga_orchestrator.repository.SagaStepLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The brain of the saga orchestrator.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Start new saga instances and dispatch the first step</li>
 *   <li>Handle step replies — advance or compensate</li>
 *   <li>Build step-specific command payloads</li>
 *   <li>Orchestrate compensation in reverse order</li>
 * </ul></p>
 */
@Service
public class SagaEngine {

    private static final Logger log = LoggerFactory.getLogger(SagaEngine.class);

    private final SagaInstanceRepository sagaRepository;
    private final SagaStepLogRepository stepLogRepository;
    private final SagaDefinitionRegistry definitionRegistry;
    private final SagaCommandPublisher commandPublisher;
    private final ObjectMapper objectMapper;

    public SagaEngine(SagaInstanceRepository sagaRepository,
                      SagaStepLogRepository stepLogRepository,
                      SagaDefinitionRegistry definitionRegistry,
                      SagaCommandPublisher commandPublisher,
                      ObjectMapper objectMapper) {
        this.sagaRepository = sagaRepository;
        this.stepLogRepository = stepLogRepository;
        this.definitionRegistry = definitionRegistry;
        this.commandPublisher = commandPublisher;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════════════════════════
    // START SAGA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start a new saga instance.
     *
     * @param request the incoming REST request
     * @return the created saga instance
     */
    @Transactional
    public SagaInstance startSaga(StartSagaRequest request) {
        String sagaType = "ORDER_SAGA";

        SagaDefinition definition = definitionRegistry.findByType(sagaType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown saga type: " + sagaType));

        // Serialize request payload to JSON
        String payloadJson = toJson(request);

        // Create saga instance
        SagaInstance saga = new SagaInstance(sagaType, payloadJson);
        saga = sagaRepository.save(saga);

        log.info("Saga created: id={}, type={}", saga.getId(), sagaType);

        // Dispatch first step
        dispatchStep(saga, definition, 0);

        saga.setStatus("RUNNING");
        saga = sagaRepository.save(saga);

        return saga;
    }

    // ═══════════════════════════════════════════════════════════════
    // HANDLE REPLY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Process a reply from a step service.
     * Determines whether to advance to the next step or start compensation.
     *
     * @param reply the step reply consumed from Kafka
     */
    @Transactional
    public void handleReply(StepReply reply) {
        UUID sagaId = UUID.fromString(reply.sagaId());

        Optional<SagaInstance> sagaOpt = sagaRepository.findById(sagaId);
        if (sagaOpt.isEmpty()) {
            log.warn("Reply for unknown saga: sagaId={}", reply.sagaId());
            return;
        }

        SagaInstance saga = sagaOpt.get();

        // Ignore replies for sagas that are already terminal
        if ("COMPLETED".equals(saga.getStatus()) || "FAILED".equals(saga.getStatus())) {
            log.info("Ignoring reply for terminal saga: sagaId={}, status={}", sagaId, saga.getStatus());
            return;
        }

        SagaDefinition definition = definitionRegistry.findByType(saga.getSagaType())
                .orElseThrow(() -> new IllegalStateException("No definition for saga type: " + saga.getSagaType()));

        // Log the reply
        logStepAction(saga, saga.getCurrentStep(), reply.stepName(),
                "REPLY", reply.isSuccess() ? "SUCCESS" : "FAILURE",
                toJson(reply), UUID.fromString(reply.idempotencyKey()));

        if (reply.isSuccess()) {
            handleSuccess(saga, definition);
        } else {
            handleFailure(saga, definition, reply);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SUCCESS PATH
    // ═══════════════════════════════════════════════════════════════

    private void handleSuccess(SagaInstance saga, SagaDefinition definition) {
        int nextStep = saga.getCurrentStep() + 1;

        if (nextStep >= definition.totalSteps()) {
            // All steps completed — saga is done!
            saga.setStatus("COMPLETED");
            sagaRepository.save(saga);
            log.info("🎉 Saga COMPLETED: sagaId={}", saga.getId());
        } else {
            // Advance to next step
            saga.setCurrentStep(nextStep);
            sagaRepository.save(saga);

            dispatchStep(saga, definition, nextStep);
            log.info("Saga advanced: sagaId={}, step={}/{}", saga.getId(), nextStep, definition.totalSteps());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FAILURE PATH — COMPENSATION
    // ═══════════════════════════════════════════════════════════════

    private void handleFailure(SagaInstance saga, SagaDefinition definition, StepReply failedReply) {
        log.warn("Step FAILED: sagaId={}, step={}, reason={}",
                saga.getId(), failedReply.stepName(), failedReply.errorMessage());

        saga.setStatus("COMPENSATING");
        sagaRepository.save(saga);

        // Compensate all completed steps in REVERSE order
        // (current step failed, so compensate steps 0..currentStep-1)
        compensateCompletedSteps(saga, definition);

        saga.setStatus("FAILED");
        sagaRepository.save(saga);
        log.info("💥 Saga FAILED (compensation dispatched): sagaId={}", saga.getId());
    }

    /**
     * Compensate all previously completed steps in reverse order.
     * Called when a step fails or when the timeout scheduler detects a stuck saga.
     */
    public void compensateCompletedSteps(SagaInstance saga, SagaDefinition definition) {
        int lastCompletedStep = saga.getCurrentStep() - 1;

        for (int i = lastCompletedStep; i >= 0; i--) {
            SagaStep step = definition.getStep(i);

            if (!step.isCompensable()) {
                log.info("Skipping non-compensable step: sagaId={}, step={}", saga.getId(), step.stepName());
                continue;
            }

            try {
                Object compensateCommand = buildCompensateCommand(saga, step, i);
                UUID idempotencyKey = UUID.randomUUID();

                commandPublisher.publish(step.compensateTopic(), saga.getId().toString(), compensateCommand);

                logStepAction(saga, i, step.stepName(), "COMPENSATE", "SENT",
                        toJson(compensateCommand), idempotencyKey);

                log.info("Compensation dispatched: sagaId={}, step={}, topic={}",
                        saga.getId(), step.stepName(), step.compensateTopic());
            } catch (Exception e) {
                log.error("Failed to dispatch compensation: sagaId={}, step={}",
                        saga.getId(), step.stepName(), e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DISPATCH STEP COMMAND
    // ═══════════════════════════════════════════════════════════════

    private void dispatchStep(SagaInstance saga, SagaDefinition definition, int stepIndex) {
        SagaStep step = definition.getStep(stepIndex);
        UUID idempotencyKey = UUID.randomUUID();

        Object command = buildStepCommand(saga, step, stepIndex, idempotencyKey);
        commandPublisher.publish(step.commandTopic(), saga.getId().toString(), command);

        logStepAction(saga, stepIndex, step.stepName(), "COMMAND", "SENT",
                toJson(command), idempotencyKey);

        log.info("Step dispatched: sagaId={}, step={}, topic={}",
                saga.getId(), step.stepName(), step.commandTopic());
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMAND BUILDERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build the command payload for a specific step.
     * Extracts data from the saga payload and previous step replies.
     */
    private Object buildStepCommand(SagaInstance saga, SagaStep step,
                                     int stepIndex, UUID idempotencyKey) {
        StartSagaRequest request = fromJson(saga.getPayload(), StartSagaRequest.class);
        String sagaId = saga.getId().toString();
        String idemKey = idempotencyKey.toString();

        return switch (step.stepName()) {
            case "RESERVE_INVENTORY" -> {
                OrderItem firstItem = request.items().getFirst();
                yield new ReserveInventoryCommand(sagaId, idemKey, firstItem.sku(), firstItem.quantity());
            }
            case "CHARGE_PAYMENT" -> {
                BigDecimal totalAmount = request.items().stream()
                        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield new ChargePaymentCommand(sagaId, idemKey, request.userId(), totalAmount);
            }
            case "SEND_NOTIFICATION" -> new SendNotificationCommand(
                    sagaId, idemKey, request.userId(), request.email(),
                    "Order Confirmation",
                    "Your order has been placed successfully!"
            );
            default -> throw new IllegalStateException("Unknown step: " + step.stepName());
        };
    }

    /**
     * Build the compensation command for a specific step.
     * Extracts data from the step's SUCCESS reply log (e.g., reservationId, txnId).
     */
    private Object buildCompensateCommand(SagaInstance saga, SagaStep step, int stepIndex) {
        String sagaId = saga.getId().toString();
        String idemKey = UUID.randomUUID().toString();

        // Find the SUCCESS reply for this step to extract output data
        Map<String, Object> stepData = getStepReplyData(saga.getId(), stepIndex);

        return switch (step.stepName()) {
            case "RESERVE_INVENTORY" -> {
                String reservationId = (String) stepData.get("reservationId");
                yield new CompensateInventoryCommand(sagaId, idemKey, reservationId);
            }
            case "CHARGE_PAYMENT" -> {
                String txnId = (String) stepData.get("txnId");
                yield new CompensatePaymentCommand(sagaId, idemKey, txnId);
            }
            default -> throw new IllegalStateException("No compensation for step: " + step.stepName());
        };
    }

    /**
     * Extract the reply data map from a step's SUCCESS reply log.
     */
    private Map<String, Object> getStepReplyData(UUID sagaId, int stepIndex) {
        List<SagaStepLog> replyLogs = stepLogRepository
                .findBySagaInstanceIdAndStepIndexAndActionTypeAndStatus(
                        sagaId, stepIndex, "REPLY", "SUCCESS");

        if (replyLogs.isEmpty()) {
            log.warn("No SUCCESS reply found for step: sagaId={}, stepIndex={}", sagaId, stepIndex);
            return Map.of();
        }

        // Parse the reply payload to extract the data map
        String replyPayload = replyLogs.getFirst().getPayload();
        StepReply reply = fromJson(replyPayload, StepReply.class);
        return reply.data() != null ? reply.data() : Map.of();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void logStepAction(SagaInstance saga, int stepIndex, String stepName,
                               String actionType, String status, String payload, UUID idempotencyKey) {
        SagaStepLog stepLog = new SagaStepLog(saga, stepIndex, stepName,
                actionType, status, payload, idempotencyKey);
        stepLogRepository.save(stepLog);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
