package application.domain.exceptions;

import application.domain.valuesObjects.OrderId;

/**
 * Raised when a return is requested over an order that has not been delivered yet: the after sales
 * cycle starts once the order is finished (RD-POS-01).
 */
public class ReturnNotAllowedException extends DomainException {

    public ReturnNotAllowedException(OrderId orderId) {
        super("order " + orderId + " has not been delivered yet and admits no return");
    }
}
