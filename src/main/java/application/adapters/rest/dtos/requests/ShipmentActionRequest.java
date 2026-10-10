package application.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound shape of a logistic action over a shipment: packing, dispatching or confirming delivery.
 *
 * <p>The warehouse and address are only needed when the shipment is being created for the first
 * time; for the later actions they are read from the stored shipment.</p>
 */
public record ShipmentActionRequest(@NotBlank String shipmentId,
                                    @NotBlank String orderId,
                                    String warehouseId,
                                    String deliveryAddress) {
}
