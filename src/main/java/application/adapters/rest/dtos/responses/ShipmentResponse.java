package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a shipment. It carries no status of its own: the logistic progress is reflected
 * on the state of the order (RD-ALC-03).
 */
public record ShipmentResponse(String id, String orderId, String warehouseId, String deliveryAddress) {
}
