package application.domain.services.authorization;

import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import java.util.Objects;

/**
 * Verifies that the cart, order or invoice the buyer acts upon is their own.
 *
 * <p>A buyer never manages information of other buyers, and an order belongs to a single buyer
 * (RD-ROL-04, RD-PED-05).</p>
 */
public class ValidateBuyerOwnershipService {

    /**
     * @throws UnauthorizedOperationException when the cart belongs to another buyer.
     */
    public void validateCart(Buyer buyer, Cart cart) {
        Objects.requireNonNull(buyer, "buyer is mandatory");
        Objects.requireNonNull(cart, "cart is mandatory");
        if (!cart.belongsTo(buyer)) {
            throw new UnauthorizedOperationException(
                    "cart " + cart.getIdentifier() + " does not belong to buyer " + buyer.getIdentifier());
        }
    }

    /**
     * @throws UnauthorizedOperationException when the order belongs to another buyer.
     */
    public void validateOrder(Buyer buyer, Order order) {
        Objects.requireNonNull(buyer, "buyer is mandatory");
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.belongsTo(buyer)) {
            throw new UnauthorizedOperationException(
                    "order " + order.getIdentifier() + " does not belong to buyer " + buyer.getIdentifier());
        }
    }

    /**
     * @throws UnauthorizedOperationException when the invoice belongs to another buyer.
     */
    public void validateInvoice(Buyer buyer, Invoice invoice) {
        Objects.requireNonNull(buyer, "buyer is mandatory");
        Objects.requireNonNull(invoice, "invoice is mandatory");
        if (!invoice.belongsTo(buyer)) {
            throw new UnauthorizedOperationException(
                    "invoice " + invoice.getIdentifier() + " does not belong to buyer " + buyer.getIdentifier());
        }
    }
}
