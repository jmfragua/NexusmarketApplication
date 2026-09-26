package application.domain.exceptions;

import application.domain.valuesObjects.CartId;

/**
 * Raised when an empty cart is confirmed: an order always holds at least one line.
 */
public class EmptyCartException extends DomainException {

    public EmptyCartException(CartId cartId) {
        super("cart " + cartId + " is empty and cannot be confirmed as an order");
    }
}
