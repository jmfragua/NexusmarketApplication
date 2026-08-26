package application.domain.models;

import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * Provisional selection of products made by a buyer. It is the initial state of the order cycle and
 * commits neither inventory nor invoicing (RD-PED-04).
 */
@Getter
public class Cart extends DomainEntity<CartId> {

    private final BuyerId buyerId;

    private final List<CartItem> items = new ArrayList<>();

    public Cart(CartId identifier, BuyerId buyerId) {
        super(identifier);
        this.buyerId = Objects.requireNonNull(buyerId, "owning buyer is mandatory");
    }

    /**
     * @return the provisional selection, as an unmodifiable view.
     */
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Selects a product variant. Selecting an already present variant accumulates the quantity
     * instead of duplicating the line.
     */
    public CartItem addItem(CartItemId itemId, ProductId productId, ProductVariantId variantId, Quantity quantity) {
        Optional<CartItem> existing = findItem(productId, variantId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.changeQuantity(item.getQuantity().add(quantity));
            return item;
        }
        CartItem item = new CartItem(itemId, productId, variantId, quantity);
        items.add(item);
        return item;
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void updateQuantity(CartItem item, Quantity quantity) {
        requireOwnItem(item).changeQuantity(quantity);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Optional<CartItem> findItem(ProductId productId, ProductVariantId variantId) {
        return items.stream()
                .filter(item -> item.selects(productId, variantId))
                .findFirst();
    }

    /**
     * Supports RG-03: the cart belongs to a single buyer.
     */
    public boolean belongsTo(Buyer buyer) {
        return buyer != null && buyerId.equals(buyer.getIdentifier());
    }

    /**
     * Confirms the selection as an order and takes it to {@code PENDING_PAYMENT}.
     *
     * <p>The lines are built beforehand with {@link CartItem#toOrderLine} because the commercial
     * value is not modelled on the product; this operation checks that they match the selection one
     * to one and empties the cart, whose selection has already been committed.</p>
     *
     * @param lines priced lines, exactly one per cart line.
     * @return the resulting order.
     */
    public Order confirm(OrderId orderId, Address deliveryAddress, List<OrderLine> lines) {
        if (isEmpty()) {
            throw new IllegalStateException("an empty cart cannot be confirmed");
        }
        Objects.requireNonNull(lines, "the lines of the order are mandatory");
        if (lines.size() != items.size()) {
            throw new IllegalArgumentException(
                    "every cart line must produce an order line: " + items.size() + " expected, " + lines.size() + " given");
        }
        items.forEach(item -> requireMatchingLine(item, lines));
        Order order = new Order(orderId, buyerId, deliveryAddress, lines);
        clear();
        return order;
    }

    private void requireMatchingLine(CartItem item, List<OrderLine> lines) {
        boolean matched = lines.stream().anyMatch(line ->
                line.getProductId().equals(item.getProductId())
                        && line.getProductVariantId().equals(item.getProductVariantId())
                        && line.getQuantity().equals(item.getQuantity()));
        if (!matched) {
            throw new IllegalArgumentException("no order line matches the cart line " + item.getIdentifier());
        }
    }

    private CartItem requireOwnItem(CartItem item) {
        if (item == null || !items.contains(item)) {
            throw new IllegalArgumentException("the line does not belong to this cart");
        }
        return item;
    }
}
