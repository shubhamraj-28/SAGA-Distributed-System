package io.shub.inventory_service.kafka;

import io.shub.inventory_service.dto.CompensateInventoryCommand;
import io.shub.inventory_service.dto.ReserveInventoryCommand;
import io.shub.inventory_service.dto.StepReply;
import io.shub.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to inventory command and compensation topics.
 *
 * <p>Flow:
 * <ol>
 *   <li>Consume {@code ReserveInventoryCommand} from {@code inventory.commands}</li>
 *   <li>Delegate to {@link InventoryService#reserveStock}</li>
 *   <li>Publish {@code StepReply} to {@code inventory.replies}</li>
 * </ol>
 * Compensation:
 * <ol>
 *   <li>Consume {@code CompensateInventoryCommand} from {@code inventory.compensate}</li>
 *   <li>Delegate to {@link InventoryService#releaseReservation}</li>
 *   <li>No reply needed for compensation</li>
 * </ol>
 * </p>
 */
@Component
public class InventoryCommandListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandListener.class);

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryCommandListener(InventoryService inventoryService,
                                     KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Handle reserve-inventory commands.
     * Processes the command and publishes a SUCCESS/FAILURE reply.
     */
    @KafkaListener(
            topics = KafkaTopics.INVENTORY_COMMANDS,
            groupId = "inventory-service-group",
            properties = {
                "spring.json.value.default.type=io.shub.inventory_service.dto.ReserveInventoryCommand"
            }
    )
    public void handleReserveCommand(ReserveInventoryCommand command) {
        log.info("Received reserve command: sagaId={}, sku={}, qty={}",
                command.sagaId(), command.sku(), command.quantity());

        try {
            StepReply reply = inventoryService.reserveStock(command);
            kafkaTemplate.send(KafkaTopics.INVENTORY_REPLIES, command.sagaId(), reply);
            log.info("Published reply: sagaId={}, status={}", command.sagaId(), reply.status());
        } catch (Exception e) {
            log.error("Error processing reserve command: sagaId={}", command.sagaId(), e);
            StepReply errorReply = StepReply.failure(
                    command.sagaId(), command.idempotencyKey(), e.getMessage());
            kafkaTemplate.send(KafkaTopics.INVENTORY_REPLIES, command.sagaId(), errorReply);
        }
    }

    /**
     * Handle inventory compensation commands.
     * Releases a previously made reservation. No reply is published.
     */
    @KafkaListener(
            topics = KafkaTopics.INVENTORY_COMPENSATE,
            groupId = "inventory-service-group",
            properties = {
                "spring.json.value.default.type=io.shub.inventory_service.dto.CompensateInventoryCommand"
            }
    )
    public void handleCompensateCommand(CompensateInventoryCommand command) {
        log.info("Received compensate command: sagaId={}, reservationId={}",
                command.sagaId(), command.reservationId());

        try {
            inventoryService.releaseReservation(command);
            log.info("Compensation completed: sagaId={}, reservationId={}",
                    command.sagaId(), command.reservationId());
        } catch (Exception e) {
            log.error("Error processing compensation: sagaId={}, reservationId={}",
                    command.sagaId(), command.reservationId(), e);
            // Compensation errors are logged but not retried via reply —
            // the orchestrator's timeout scheduler will catch stuck compensations.
        }
    }
}
