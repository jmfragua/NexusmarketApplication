package application.domain.models;

import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import java.util.Objects;
import lombok.Getter;

/**
 * Line of provisional selection inside the cart.
 */
@Getter
public class CartItem extends DomainEntity<CartItemId> {

    private final ProductId productId;

    private final ProductVariantId productVariantId;

    private Quantity quantity;

    public CartItem(CartItemId identifier, ProductId productId, ProductVariantId productVariantId, Quantity quantity) {
        super(identifier);
        this.productId = Objects.requireNonNull(productId, "product is mandatory");
        this.productVariantId = Objects.requireNonNull(productVariantId, "product variant is mandatory");
        this.quantity = requirePositive(quantity);
    }

    public void changeQuantity(Quantity newQuantity) {
        this.quantity = requirePositive(newQuantity);
    }

    /**
     * Converts the selection into a committed order line when the cart is confirmed.
     *
     * <p>The commercial value is supplied by the caller: the business specification models no price
     * on the product, only the amount of the order line, so the domain never invents it
     * (RD-ALC-03).</p>
     *
     * @param lineAmount commercial value of the resulting line.
     */
    public OrderLine toOrderLine(OrderLineId lineId, OrderId orderId, Money lineAmount) {
        return new OrderLine(lineId, orderId, productId, productVariantId, quantity, lineAmount);
    }

    /**
     * @return true when the line selects the same product variant.
     */
    public boolean selects(ProductId product, ProductVariantId variant) {
        return productId.equals(product) && productVariantId.equals(variant);
    }

    private static Quantity requirePositive(Quantity quantity) {
        Objects.requireNonNull(quantity, "quantity is mandatory");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("a cart line cannot be of zero units");
        }
        return quantity;
    }
}
