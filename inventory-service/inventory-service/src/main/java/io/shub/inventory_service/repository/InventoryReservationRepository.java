package io.shub.inventory_service.repository;

import io.shub.inventory_service.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link InventoryReservation} entities.
 */
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    /**
     * Check if a reservation with this idempotency key already exists.
     * Used to enforce exactly-once processing of saga commands.
     */
    Optional<InventoryReservation> findByIdempotencyKey(UUID idempotencyKey);

    /**
     * Find a reservation by its unique reservation ID.
     * Used during compensation to release the reservation.
     */
    Optional<InventoryReservation> findByReservationId(UUID reservationId);
}
