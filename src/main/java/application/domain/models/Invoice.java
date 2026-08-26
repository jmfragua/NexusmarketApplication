package application.domain.models;

import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import java.util.Objects;
import lombok.Getter;

/**
 * Commercial information tied to a sale. Documents the invoicing of the purchase.
 *
 * <p>The invoice is generated out of the paid order and reflects its commercial value
 * (RD-FAC-01).</p>
 */
@Getter
public class Invoice extends DomainEntity<InvoiceId> {

    private final OrderId orderId;

    private final BuyerId buyerId;

    private final Money totalAmount;

    public Invoice(InvoiceId identifier, OrderId orderId, BuyerId buyerId, Money totalAmount) {
        super(identifier);
        this.orderId = Objects.requireNonNull(orderId, "invoiced order is mandatory");
        this.buyerId = Objects.requireNonNull(buyerId, "invoiced buyer is mandatory");
        this.totalAmount = Objects.requireNonNull(totalAmount, "invoiced total is mandatory");
    }

    /**
     * Issues the invoice of an order once its payment has been validated (RD-FAC-01).
     *
     * @throws IllegalStateException when the payment of the order has not been confirmed yet.
     */
    public static Invoice issueFor(InvoiceId identifier, Order order) {
        Objects.requireNonNull(order, "order is mandatory");
        if (order.getStatus() == OrderStatus.CART || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("an order is only invoiced once its payment is validated");
        }
        return new Invoice(identifier, order.getIdentifier(), order.getBuyerId(), order.getTotalAmount());
    }

    /**
     * @return true when the invoice is consistent with the order it documents.
     */
    public boolean matchesOrderTotal(Order order) {
        return order != null
                && orderId.equals(order.getIdentifier())
                && totalAmount.equals(order.getTotalAmount());
    }

    public boolean belongsTo(Buyer buyer) {
        return buyer != null && buyerId.equals(buyer.getIdentifier());
    }
}
