package application.domain.models;

import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Objects;

/**
 * Digital product. Needs neither inventory nor dispatch: it is delivered immediately once the
 * payment is confirmed (RD-INV-05, RD-LOG-01).
 */
public class DigitalProduct extends Product {

    public DigitalProduct(ProductId identifier, SellerId sellerId) {
        super(identifier, sellerId, ProductType.DIGITAL);
    }

    private DigitalProduct(ProductId identifier,
                           SellerId sellerId,
                           ProductStatus status,
                           List<ProductVariant> variants) {
        super(identifier, sellerId, ProductType.DIGITAL, status, variants);
    }

    /**
     * Restores a digital product that already existed, with the state and variants it had been
     * persisted in (RD-ALC-04).
     */
    public static DigitalProduct reconstruct(ProductId identifier,
                                             SellerId sellerId,
                                             ProductStatus status,
                                             List<ProductVariant> variants) {
        return new DigitalProduct(identifier, sellerId, status, variants);
    }

    @Override
    public boolean requiresInventory() {
        return false;
    }

    /**
     * Delivers the product as soon as the payment of the order is confirmed.
     *
     * @throws IllegalStateException when the order has not been paid yet, or does not contain this
     *                               product.
     */
    public void deliverOnPayment(Order order) {
        Objects.requireNonNull(order, "order is mandatory");
        if (order.getStatus() == OrderStatus.CART || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("a digital product is only delivered once the payment is confirmed");
        }
        if (!order.contains(getIdentifier())) {
            throw new IllegalStateException("the order does not contain product " + getIdentifier());
        }
    }
}
