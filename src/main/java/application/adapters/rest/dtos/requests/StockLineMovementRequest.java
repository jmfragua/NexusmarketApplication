package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a stock operation driven by an order line: reserving, releasing or issuing for
 * sale. The quantity is not sent — it comes from the line itself, so the commitment can never drift
 * from what was ordered.
 */
public record StockLineMovementRequest(@NotBlank String orderId,
                                       @NotBlank String orderLineId,
                                       @NotBlank String inventoryItemId,
                                       @NotBlank String movementId) {
}
