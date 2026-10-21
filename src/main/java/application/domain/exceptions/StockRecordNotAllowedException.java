package application.domain.exceptions;

import application.domain.valuesObjects.ProductId;

/**
 * Raised when a stock record is opened for a product that cannot hold stock: a digital product never
 * generates stock (RD-INV-05), and a discontinued product is definitively withdrawn (RD-CAT-04).
 */
public class StockRecordNotAllowedException extends DomainException {

    private StockRecordNotAllowedException(String message) {
        super(message);
    }

    public static StockRecordNotAllowedException digitalProduct(ProductId productId) {
        return new StockRecordNotAllowedException(
                "product " + productId + " is digital and digital products hold no stock");
    }

    public static StockRecordNotAllowedException discontinuedProduct(ProductId productId) {
        return new StockRecordNotAllowedException(
                "product " + productId + " is discontinued and can no longer hold stock");
    }
}
