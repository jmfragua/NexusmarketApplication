package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of an after sales return.
 */
public record ProductReturnResponse(String id, String orderId, String orderLineId, int quantity) {
}
