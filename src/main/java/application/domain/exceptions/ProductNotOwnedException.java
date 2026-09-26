package application.domain.exceptions;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.SellerId;

/**
 * Raised when a seller tries to manage a product of another seller: every product belongs to a
 * single seller and only that seller manages it (RD-CAT-01, RG-03).
 */
public class ProductNotOwnedException extends DomainException {

    public ProductNotOwnedException(SellerId sellerId, ProductId productId) {
        super("product " + productId + " does not belong to seller " + sellerId);
    }
}
