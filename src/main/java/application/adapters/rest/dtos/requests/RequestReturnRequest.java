package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Inbound shape of an after sales return request, always originated in a line of a delivered order
 * (RD-POS-01).
 */
public record RequestReturnRequest(@NotBlank String returnId,
                                   @NotBlank String orderId,
                                   @NotBlank String orderLineId,
                                   @Positive int quantity) {
}
