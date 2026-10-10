package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a line of the provisional selection.
 */
public record CartItemResponse(String id, String productId, String productVariantId, int quantity) {
}
