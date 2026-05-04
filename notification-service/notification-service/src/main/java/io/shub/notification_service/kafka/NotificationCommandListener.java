package io.shub.notification_service.kafka;

import io.shub.notification_service.dto.SendNotificationCommand;
import io.shub.notification_service.dto.StepReply;
import io.shub.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to notification command topic.
 *
 * <p>Flow:
 * <ol>
 *   <li>Consume {@code SendNotificationCommand} from {@code notification.commands}</li>
 *   <li>Delegate to {@link NotificationService#sendNotification}</li>
 *   <li>Publish SUCCESS/FAILURE {@code StepReply} to {@code notification.replies}</li>
 * </ol>
 *
 * <p><b>No compensation topic exists</b> — you can't un-send an email.
 * This is why notification is the last step in the saga. If it fails,
 * the orchestrator compensates all previous steps.</p>
 */
@Component
public class NotificationCommandListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationCommandListener.class);

    private final NotificationService notificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationCommandListener(NotificationService notificationService,
                                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.notificationService = notificationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Handle send-notification commands.
     * Attempts to send the notification and publishes a reply.
     */
    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_COMMANDS,
            groupId = "notification-service-group",
            properties = {
                "spring.json.value.default.type=io.shub.notification_service.dto.SendNotificationCommand"
            }
    )
    public void handleSendCommand(SendNotificationCommand command) {
        log.info("Received notification command: sagaId={}, userId={}, email={}",
                command.sagaId(), command.userId(), command.email());

        try {
            notificationService.sendNotification(
                    command.userId(), command.email(), command.subject(), command.message());

            StepReply reply = StepReply.success(command.sagaId(), command.idempotencyKey());
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_REPLIES, command.sagaId(), reply);
            log.info("Published reply: sagaId={}, status=SUCCESS", command.sagaId());

        } catch (Exception e) {
            log.error("Notification failed: sagaId={}, reason={}", command.sagaId(), e.getMessage());
            StepReply reply = StepReply.failure(
                    command.sagaId(), command.idempotencyKey(), e.getMessage());
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_REPLIES, command.sagaId(), reply);
            log.info("Published reply: sagaId={}, status=FAILURE", command.sagaId());
        }
    }
}
