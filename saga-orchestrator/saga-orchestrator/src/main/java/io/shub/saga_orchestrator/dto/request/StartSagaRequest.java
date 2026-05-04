package io.shub.saga_orchestrator.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * REST request body to trigger a new saga instance.
 * Example:
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
public record StartSagaRequest(
        @NotBlank String userId,
        @Email @NotBlank String email,
        @NotEmpty List<OrderItem> items
) {}
