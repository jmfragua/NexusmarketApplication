package application.domain.exceptions;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;

/**
 * Raised when an operation would leave the stock below zero or commits more units than available:
 * negative stock is never allowed and stock that does not exist can never be reserved (RD-INV-02,
 * RD-INV-03).
 */
public class InsufficientStockException extends DomainException {

    public InsufficientStockException(ProductId productId, Quantity available, Quantity requested) {
        super("product " + productId + " has not enough stock: available " + available + ", requested " + requested);
    }
}
