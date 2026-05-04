package io.shub.payment_service.repository;

import io.shub.payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PaymentTransaction} entities.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Check if a transaction with this idempotency key already exists.
     * Used to enforce exactly-once processing of saga commands.
     */
    Optional<PaymentTransaction> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Find a transaction by its unique transaction ID.
     * Used during compensation to refund the charge.
     */
    Optional<PaymentTransaction> findByTxnId(UUID txnId);
}
