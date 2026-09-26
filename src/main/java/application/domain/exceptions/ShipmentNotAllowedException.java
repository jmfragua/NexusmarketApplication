package application.domain.exceptions;

import application.domain.valuesObjects.OrderId;

/**
 * Raised when a shipment is generated for an order holding no physical products: digital products
 * are delivered immediately after the payment and never generate a shipment (RD-LOG-01).
 */
public class ShipmentNotAllowedException extends DomainException {

    public ShipmentNotAllowedException(OrderId orderId) {
        super("order " + orderId + " holds no physical products and generates no shipment");
    }
}
