package application.domain.models;

import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import java.util.Objects;
import lombok.Getter;

/**
 * Refund tied to a return. Shared responsibility between the buyer, who requests it, and the
 * administrator, who manages it.
 *
 * <p>Every refund derives from a return and is applied on the invoice of the corresponding order
 * (RD-POS-02).</p>
 */
@Getter
public class Refund extends DomainEntity<RefundId> {

    private final ProductReturnId productReturnId;

    private final InvoiceId invoiceId;

    private final Money amount;

    public Refund(RefundId identifier, ProductReturnId productReturnId, InvoiceId invoiceId, Money amount) {
        super(identifier);
        this.productReturnId = Objects.requireNonNull(productReturnId, "originating return is mandatory");
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoice is mandatory");
        this.amount = Objects.requireNonNull(amount, "refunded amount is mandatory");
    }

    /**
     * Issues the refund of a return, applied on the invoice of the order it originates from
     * (RD-POS-02).
     *
     * @throws IllegalArgumentException when the invoice does not document the order of the return,
     *                                  or the amount exceeds the invoiced total.
     */
    public static Refund issueFor(RefundId identifier, ProductReturn productReturn, Invoice invoice, Money amount) {
        Objects.requireNonNull(productReturn, "return is mandatory");
        Objects.requireNonNull(invoice, "invoice is mandatory");
        Objects.requireNonNull(amount, "refunded amount is mandatory");
        if (!invoice.getOrderId().equals(productReturn.getOrderId())) {
            throw new IllegalArgumentException("the invoice does not document the order of the return");
        }
        Refund refund = new Refund(identifier, productReturn.getIdentifier(), invoice.getIdentifier(), amount);
        if (!refund.amountWithinInvoiceTotal(invoice)) {
            throw new IllegalArgumentException("the refunded amount exceeds the invoiced total");
        }
        return refund;
    }

    /**
     * @return true when the refunded amount does not exceed the total of the invoice it is applied
     *         on.
     */
    public boolean amountWithinInvoiceTotal(Invoice invoice) {
        return invoice != null
                && invoiceId.equals(invoice.getIdentifier())
                && !amount.isGreaterThan(invoice.getTotalAmount());
    }

    public boolean derivesFrom(ProductReturn productReturn) {
        return productReturn != null && productReturnId.equals(productReturn.getIdentifier());
    }
}
