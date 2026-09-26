package application.domain.exceptions;

import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;

/**
 * Raised when an order is moved outside its sequential cycle: the cycle admits neither jumps nor
 * steps backwards (RD-PED-01).
 */
public class InvalidOrderTransitionException extends DomainException {

    public InvalidOrderTransitionException(OrderId orderId, OrderStatus current, OrderStatus target) {
        super("order " + orderId + " cannot move from " + current + " to " + target
                + ": the cycle is sequential and admits neither jumps nor steps backwards");
    }
}
