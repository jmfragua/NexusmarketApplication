package application.domain.exceptions;

import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;

/**
 * Raised when a refund exceeds the total of the invoice it is applied on (RD-POS-02).
 */
public class RefundExceedsInvoiceException extends DomainException {

    public RefundExceedsInvoiceException(InvoiceId invoiceId, Money amount) {
        super("the refunded amount " + amount + " exceeds the total invoiced in " + invoiceId);
    }
}
