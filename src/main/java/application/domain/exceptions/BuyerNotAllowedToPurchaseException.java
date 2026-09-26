package application.domain.exceptions;

import application.domain.valuesObjects.BuyerId;

/**
 * Raised when a buyer whose commercial status does not allow purchases tries to confirm an order.
 */
public class BuyerNotAllowedToPurchaseException extends DomainException {

    public BuyerNotAllowedToPurchaseException(BuyerId buyerId) {
        super("buyer " + buyerId + " is not allowed to place purchases");
    }
}
