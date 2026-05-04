package io.shub.saga_orchestrator.controller;

import io.shub.saga_orchestrator.dto.request.StartSagaRequest;
import io.shub.saga_orchestrator.dto.response.SagaResponse;
import io.shub.saga_orchestrator.engine.SagaEngine;
import io.shub.saga_orchestrator.entity.SagaInstance;
import io.shub.saga_orchestrator.entity.SagaStepLog;
import io.shub.saga_orchestrator.repository.SagaInstanceRepository;
import io.shub.saga_orchestrator.repository.SagaStepLogRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for triggering and querying saga instances.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/sagas} — trigger a new saga</li>
 *   <li>{@code GET /api/sagas/{id}} — get saga status</li>
 *   <li>{@code GET /api/sagas/{id}/logs} — get saga step audit log</li>
 *   <li>{@code GET /api/sagas} — list all sagas</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/api/sagas")
public class SagaController {

    private static final Logger log = LoggerFactory.getLogger(SagaController.class);

    private final SagaEngine sagaEngine;
    private final SagaInstanceRepository sagaRepository;
    private final SagaStepLogRepository stepLogRepository;

    public SagaController(SagaEngine sagaEngine,
                          SagaInstanceRepository sagaRepository,
                          SagaStepLogRepository stepLogRepository) {
        this.sagaEngine = sagaEngine;
        this.sagaRepository = sagaRepository;
        this.stepLogRepository = stepLogRepository;
    }

    /**
     * Trigger a new saga instance.
     *
     * <pre>
     * POST /api/sagas
     * {
     *   "userId": "user-42",
     *   "email": "user@example.com",
     *   "items": [
     *     { "sku": "SKU-MOUSE-001", "quantity": 2, "price": 29.99 }
     *   ]
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<SagaResponse> startSaga(@Valid @RequestBody StartSagaRequest request) {
        log.info("Starting saga: userId={}, items={}", request.userId(), request.items().size());

        SagaInstance saga = sagaEngine.startSaga(request);

        SagaResponse response = toResponse(saga);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the current status of a saga instance.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SagaResponse> getSaga(@PathVariable UUID id) {
        return sagaRepository.findById(id)
                .map(saga -> ResponseEntity.ok(toResponse(saga)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the full step audit log for a saga instance.
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<Map<String, Object>>> getSagaLogs(@PathVariable UUID id) {
        if (sagaRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<SagaStepLog> logs = stepLogRepository.findBySagaInstanceIdOrderByCreatedAtAsc(id);

        List<Map<String, Object>> response = logs.stream()
                .map(entry -> Map.<String, Object>of(
                        "stepIndex", entry.getStepIndex(),
                        "stepName", entry.getStepName(),
                        "actionType", entry.getActionType(),
                        "status", entry.getStatus(),
                        "idempotencyKey", entry.getIdempotencyKey().toString(),
                        "createdAt", entry.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * List all saga instances.
     */
    @GetMapping
    public ResponseEntity<List<SagaResponse>> listSagas() {
        List<SagaResponse> sagas = sagaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(sagas);
    }

    private SagaResponse toResponse(SagaInstance saga) {
        return new SagaResponse(
                saga.getId().toString(),
                saga.getSagaType(),
                saga.getStatus(),
                saga.getCurrentStep(),
                saga.getCreatedAt(),
                saga.getUpdatedAt()
        );
    }
}
