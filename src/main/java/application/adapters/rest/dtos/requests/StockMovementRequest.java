package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Inbound shape of a stock operation that moves a quantity: receiving stock or adjusting it.
 *
 * <p>The movement identifier travels in the request because the caller of the use case supplies the
 * identifiers of new entities (RD-POUT-04), and every stock change materializes as a movement
 * (RD-INV-04).</p>
 */
public record StockMovementRequest(@NotBlank String inventoryItemId,
                                   @NotBlank String movementId,
                                   @Positive int quantity) {
}
