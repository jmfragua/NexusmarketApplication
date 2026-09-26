package application.domain.exceptions;

import application.domain.valuesObjects.OrderId;

/**
 * Raised when an operation needs the invoice of an order that has not been invoiced yet: invoicing
 * is generated out of the paid order (RD-FAC-01).
 */
public class InvoiceNotIssuedException extends DomainException {

    public InvoiceNotIssuedException(OrderId orderId) {
        super("order " + orderId + " has not been invoiced yet");
    }
}
