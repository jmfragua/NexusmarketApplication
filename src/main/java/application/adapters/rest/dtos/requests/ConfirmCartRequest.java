package application.adapters.rest.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Inbound shape of a cart confirmation: the first transition of the order lifecycle (RD-PED-01).
 */
public record ConfirmCartRequest(@NotBlank String orderId,
                                 @NotBlank String deliveryAddress,
                                 @NotEmpty @Valid List<OrderLineRequest> lines) {
}
