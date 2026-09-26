package application.domain.exceptions;

import application.domain.valuesObjects.ProductId;

/**
 * Raised when stock marked as damaged is committed: damaged stock is excluded from every
 * reservation (RD-INV-03).
 */
public class DamagedStockException extends DomainException {

    public DamagedStockException(ProductId productId) {
        super("the stock of product " + productId + " is damaged and cannot be reserved");
    }
}
