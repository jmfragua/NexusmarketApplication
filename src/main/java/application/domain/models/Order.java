package application.domain.models;

import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Purchase request made by a buyer. Represents the formal commercial commitment and its lifecycle
 * is the central process of the system.
 *
 * <p>The state cycle is strictly sequential and admits neither jumps nor steps backwards
 * (RD-PED-01), and a finished order can never be modified under any circumstance
 * (RD-PED-02).</p>
 */
@Getter
public class Order extends DomainEntity<OrderId> {

    private final BuyerId buyerId;

    private final List<OrderLine> lines = new ArrayList<>();

    private final Money totalAmount;

    private Address deliveryAddress;

    private OrderStatus status;

    /**
     * Builds the order out of a confirmed cart, so it is born in {@code PENDING_PAYMENT}: the
     * {@code CART} state of the cycle is the one held by the {@link Cart} entity (RD-PED-04).
     */
    public Order(OrderId identifier, BuyerId buyerId, Address deliveryAddress, List<OrderLine> lines) {
        super(identifier);
        this.buyerId = Objects.requireNonNull(buyerId, "buyer is mandatory");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "delivery address is mandatory");
        Objects.requireNonNull(lines, "the lines of the order are mandatory");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order holds at least one line");
        }
        lines.forEach(this::addLine);
        this.totalAmount = calculateTotal();
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    /**
     * @return the lines of the order, as an unmodifiable view.
     */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Confirms the payment and starts the preparation processes (RD-PED-03).
     */
    public void confirmPayment() {
        changeStatus(OrderStatus.PAID);
    }

    /**
     * Registers the physical exit from the warehouse.
     */
    public void dispatch() {
        changeStatus(OrderStatus.DISPATCHED);
    }

    /**
     * Closes the order once the delivery is confirmed. {@code DELIVERED} is terminal.
     */
    public void completeDelivery() {
        changeStatus(OrderStatus.DELIVERED);
    }

    /**
     * Changes the delivery location while the order can still be modified.
     */
    public void changeDeliveryAddress(Address address) {
        requireModifiable();
        this.deliveryAddress = Objects.requireNonNull(address, "delivery address is mandatory");
    }

    /**
     * A finished order cannot be modified under any circumstance (RD-PED-02).
     */
    public boolean isModifiable() {
        return status.isModifiable();
    }

    /**
     * Supports RG-03 and RD-PED-05: an order belongs to a single buyer.
     */
    public boolean belongsTo(Buyer buyer) {
        return buyer != null && buyerId.equals(buyer.getIdentifier());
    }

    /**
     * Determines whether the order needs logistics: only orders holding physical products generate
     * shipments (RD-LOG-01).
     *
     * <p>The lines hold no product type, so the referenced products decide.</p>
     *
     * @param products the products referenced by the lines of this order.
     */
    public boolean containsPhysicalProducts(Collection<Product> products) {
        Objects.requireNonNull(products, "the products of the order are mandatory");
        return products.stream()
                .filter(product -> contains(product.getIdentifier()))
                .anyMatch(Product::requiresInventory);
    }

    public boolean contains(ProductId productId) {
        return lines.stream().anyMatch(line -> line.getProductId().equals(productId));
    }

    private void addLine(OrderLine line) {
        Objects.requireNonNull(line, "order line is mandatory");
        if (!line.getOrderId().equals(getIdentifier())) {
            throw new IllegalArgumentException("the line belongs to another order");
        }
        lines.add(line);
    }

    private Money calculateTotal() {
        return lines.stream()
                .map(OrderLine::getLineAmount)
                .reduce(Money::add)
                .orElseThrow(() -> new IllegalStateException("an order holds at least one line"));
    }

    private void changeStatus(OrderStatus target) {
        requireModifiable();
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("the order cannot move from " + status + " to " + target
                    + ": the cycle is sequential and admits neither jumps nor steps backwards");
        }
        this.status = target;
    }

    private void requireModifiable() {
        if (!isModifiable()) {
            throw new IllegalStateException("a delivered order cannot be modified under any circumstance");
        }
    }
}
