package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Inbound shape of a selection added to the cart.
 */
public record AddCartItemRequest(@NotBlank String cartItemId,
                                 @NotBlank String productId,
                                 @NotBlank String productVariantId,
                                 @Positive int quantity) {
}
