package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Positive;

/**
 * Inbound shape of a quantity change on a cart line. Zero is rejected here and again by the domain:
 * removing a line is a different operation.
 */
public record UpdateCartItemQuantityRequest(@Positive int quantity) {
}
