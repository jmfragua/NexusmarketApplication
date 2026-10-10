package application.adapters.rest.dtos.responses;

import java.util.List;

/**
 * Outbound shape of a cart.
 */
public record CartResponse(String id, String buyerId, List<CartItemResponse> items) {
}
