package application.domain.exceptions;

import application.domain.valuesObjects.OrderId;

/**
 * Raised when a finished order is modified: a delivered order can never be modified under any
 * circumstance (RD-PED-02).
 */
public class OrderNotModifiableException extends DomainException {

    public OrderNotModifiableException(OrderId orderId) {
        super("order " + orderId + " is finished and cannot be modified under any circumstance");
    }
}
