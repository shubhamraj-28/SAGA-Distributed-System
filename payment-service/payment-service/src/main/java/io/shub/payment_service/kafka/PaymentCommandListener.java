package io.shub.payment_service.kafka;

import io.shub.payment_service.dto.ChargePaymentCommand;
import io.shub.payment_service.dto.CompensatePaymentCommand;
import io.shub.payment_service.dto.StepReply;
import io.shub.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to payment command and compensation topics.
 *
 * <p>Flow:
 * <ol>
 *   <li>Consume {@code ChargePaymentCommand} from {@code payment.commands}</li>
 *   <li>Delegate to {@link PaymentService#chargePayment}</li>
 *   <li>Publish {@code StepReply} to {@code payment.replies}</li>
 * </ol>
 * Compensation:
 * <ol>
 *   <li>Consume {@code CompensatePaymentCommand} from {@code payment.compensate}</li>
 *   <li>Delegate to {@link PaymentService#refundPayment}</li>
 *   <li>No reply needed for compensation</li>
 * </ol>
 * </p>
 */
@Component
public class PaymentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentCommandListener(PaymentService paymentService,
                                   KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Handle charge-payment commands.
     * Processes the command and publishes a SUCCESS/FAILURE reply.
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMMANDS,
            groupId = "payment-service-group",
            properties = {
                "spring.json.value.default.type=io.shub.payment_service.dto.ChargePaymentCommand"
            }
    )
    public void handleChargeCommand(ChargePaymentCommand command) {
        log.info("Received charge command: sagaId={}, userId={}, amount={}",
                command.sagaId(), command.userId(), command.amount());

        try {
            StepReply reply = paymentService.chargePayment(command);
            kafkaTemplate.send(KafkaTopics.PAYMENT_REPLIES, command.sagaId(), reply);
            log.info("Published reply: sagaId={}, status={}", command.sagaId(), reply.status());
        } catch (Exception e) {
            log.error("Error processing charge command: sagaId={}", command.sagaId(), e);
            StepReply errorReply = StepReply.failure(
                    command.sagaId(), command.idempotencyKey(), e.getMessage());
            kafkaTemplate.send(KafkaTopics.PAYMENT_REPLIES, command.sagaId(), errorReply);
        }
    }

    /**
     * Handle payment compensation commands.
     * Refunds a previously charged transaction. No reply is published.
     */
    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPENSATE,
            groupId = "payment-service-group",
            properties = {
                "spring.json.value.default.type=io.shub.payment_service.dto.CompensatePaymentCommand"
            }
    )
    public void handleCompensateCommand(CompensatePaymentCommand command) {
        log.info("Received compensate command: sagaId={}, txnId={}",
                command.sagaId(), command.txnId());

        try {
            paymentService.refundPayment(command);
            log.info("Compensation completed: sagaId={}, txnId={}",
                    command.sagaId(), command.txnId());
        } catch (Exception e) {
            log.error("Error processing compensation: sagaId={}, txnId={}",
                    command.sagaId(), command.txnId(), e);
        }
    }
}
