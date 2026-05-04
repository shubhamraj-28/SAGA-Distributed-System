package io.shub.payment_service.service;

import io.shub.payment_service.dto.ChargePaymentCommand;
import io.shub.payment_service.dto.CompensatePaymentCommand;
import io.shub.payment_service.dto.StepReply;
import io.shub.payment_service.entity.PaymentTransaction;
import io.shub.payment_service.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for payment operations.
 * Handles charging and refunding with idempotency guarantees.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository transactionRepository;

    public PaymentService(PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Charge the user for the given amount.
     *
     * <p><b>Idempotency:</b> If a transaction with the same {@code idempotencyKey}
     * already exists, return the previously created transaction's reply without
     * charging again.</p>
     *
     * @param command the charge command from the orchestrator
     * @return a StepReply indicating SUCCESS or FAILURE
     */
    @Transactional
    public StepReply chargePayment(ChargePaymentCommand command) {
        UUID idempotencyKey = UUID.fromString(command.idempotencyKey());

        // ── Idempotency check ──
        Optional<PaymentTransaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent hit: transaction already exists for key={}", idempotencyKey);
            PaymentTransaction txn = existing.get();
            return StepReply.success(command.sagaId(), command.idempotencyKey(),
                    Map.of("txnId", txn.getTxnId().toString()));
        }

        // ── Validate amount ──
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid payment amount: userId={}, amount={}", command.userId(), command.amount());
            return StepReply.failure(command.sagaId(), command.idempotencyKey(),
                    "Invalid payment amount: " + command.amount());
        }

        // ── Create transaction (simulates charging) ──
        UUID txnId = UUID.randomUUID();
        UUID sagaId = UUID.fromString(command.sagaId());

        PaymentTransaction transaction = new PaymentTransaction(
                txnId, sagaId, command.userId(), command.amount(), "CHARGED", idempotencyKey
        );
        transactionRepository.save(transaction);

        log.info("Payment charged: userId={}, amount={}, txnId={}", command.userId(), command.amount(), txnId);

        return StepReply.success(command.sagaId(), command.idempotencyKey(),
                Map.of("txnId", txnId.toString()));
    }

    /**
     * Refund a previously charged transaction (compensation).
     *
     * <p><b>Idempotency:</b> If the transaction is already REFUNDED, this is a no-op.</p>
     *
     * @param command the compensation command from the orchestrator
     */
    @Transactional
    public void refundPayment(CompensatePaymentCommand command) {
        UUID txnId = UUID.fromString(command.txnId());

        Optional<PaymentTransaction> txnOpt = transactionRepository.findByTxnId(txnId);
        if (txnOpt.isEmpty()) {
            log.warn("Compensation: transaction not found: txnId={}", txnId);
            return;
        }

        PaymentTransaction transaction = txnOpt.get();

        // ── Idempotency: already refunded ──
        if ("REFUNDED".equals(transaction.getStatus())) {
            log.info("Compensation idempotent hit: transaction already refunded: txnId={}", txnId);
            return;
        }

        // ── Refund (mark as refunded) ──
        transaction.setStatus("REFUNDED");
        transactionRepository.save(transaction);

        log.info("Payment refunded: txnId={}, userId={}, amount={}",
                txnId, transaction.getUserId(), transaction.getAmount());
    }
}
