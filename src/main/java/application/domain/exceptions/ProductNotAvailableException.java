package application.domain.exceptions;

import application.domain.valuesObjects.ProductId;

/**
 * Raised when a product that is not visible in the public catalogue is selected: only a published
 * product holding at least one variant can be sold (RD-CAT-03, RD-CAT-05).
 */
public class ProductNotAvailableException extends DomainException {

    public ProductNotAvailableException(ProductId productId) {
        super("product " + productId + " is not available in the public catalogue");
    }
}
