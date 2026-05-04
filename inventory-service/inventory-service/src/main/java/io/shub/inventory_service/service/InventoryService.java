package io.shub.inventory_service.service;

import io.shub.inventory_service.dto.CompensateInventoryCommand;
import io.shub.inventory_service.dto.ReserveInventoryCommand;
import io.shub.inventory_service.dto.StepReply;
import io.shub.inventory_service.entity.InventoryReservation;
import io.shub.inventory_service.entity.Product;
import io.shub.inventory_service.repository.InventoryReservationRepository;
import io.shub.inventory_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for inventory operations.
 * Handles stock reservation and compensation (release) with idempotency guarantees.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(ProductRepository productRepository,
                            InventoryReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Reserve stock for the given SKU.
     *
     * <p><b>Idempotency:</b> If a reservation with the same {@code idempotencyKey}
     * already exists, return the previously created reservation's reply without
     * modifying stock again.</p>
     *
     * @param command the reserve command from the orchestrator
     * @return a StepReply indicating SUCCESS or FAILURE
     */
    @Transactional
    public StepReply reserveStock(ReserveInventoryCommand command) {
        UUID idempotencyKey = UUID.fromString(command.idempotencyKey());

        // ── Idempotency check ──
        Optional<InventoryReservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent hit: reservation already exists for key={}", idempotencyKey);
            InventoryReservation r = existing.get();
            return StepReply.success(command.sagaId(), command.idempotencyKey(),
                    Map.of("reservationId", r.getReservationId().toString()));
        }

        // ── Find product by SKU ──
        Optional<Product> productOpt = productRepository.findBySku(command.sku());
        if (productOpt.isEmpty()) {
            log.warn("Product not found: sku={}", command.sku());
            return StepReply.failure(command.sagaId(), command.idempotencyKey(),
                    "Product not found: " + command.sku());
        }

        Product product = productOpt.get();

        // ── Check available stock ──
        if (product.getAvailableQty() < command.quantity()) {
            log.warn("Insufficient stock: sku={}, available={}, requested={}",
                    command.sku(), product.getAvailableQty(), command.quantity());
            return StepReply.failure(command.sagaId(), command.idempotencyKey(),
                    "Insufficient stock for SKU: " + command.sku());
        }

        // ── Reserve stock ──
        product.setAvailableQty(product.getAvailableQty() - command.quantity());
        productRepository.save(product);

        UUID reservationId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation(
                reservationId, product, command.quantity(), "RESERVED", idempotencyKey
        );
        reservationRepository.save(reservation);

        log.info("Stock reserved: sku={}, qty={}, reservationId={}", command.sku(), command.quantity(), reservationId);

        return StepReply.success(command.sagaId(), command.idempotencyKey(),
                Map.of("reservationId", reservationId.toString()));
    }

    /**
     * Release a previously made reservation (compensation).
     *
     * <p><b>Idempotency:</b> If the reservation is already RELEASED, this is a no-op.</p>
     *
     * @param command the compensation command from the orchestrator
     */
    @Transactional
    public void releaseReservation(CompensateInventoryCommand command) {
        UUID reservationId = UUID.fromString(command.reservationId());

        Optional<InventoryReservation> reservationOpt = reservationRepository.findByReservationId(reservationId);
        if (reservationOpt.isEmpty()) {
            log.warn("Compensation: reservation not found: reservationId={}", reservationId);
            return;
        }

        InventoryReservation reservation = reservationOpt.get();

        // ── Idempotency: already released ──
        if ("RELEASED".equals(reservation.getStatus())) {
            log.info("Compensation idempotent hit: reservation already released: reservationId={}", reservationId);
            return;
        }

        // ── Release stock back ──
        Product product = reservation.getProduct();
        product.setAvailableQty(product.getAvailableQty() + reservation.getQuantity());
        productRepository.save(product);

        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);

        log.info("Stock released: reservationId={}, sku={}, qty={}",
                reservationId, product.getSku(), reservation.getQuantity());
    }
}
