package application.domain.models;

import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * User who acquires published products. Manages only the information needed to take part in
 * commercial processes.
 *
 * <p>A buyer never manages information of other buyers nor inventories (RD-ROL-04).</p>
 */
@Getter
public class Buyer extends User<BuyerId> {

    private Address primaryAddress;

    private final List<Address> additionalAddresses = new ArrayList<>();

    private BuyerCommercialStatus commercialStatus;

    public Buyer(BuyerId identifier,
                 FullName fullName,
                 EmailAddress email,
                 IdentityDocument identityDocument,
                 UserStatus status,
                 Address primaryAddress,
                 BuyerCommercialStatus commercialStatus) {
        super(identifier, fullName, email, identityDocument, UserRole.BUYER, status);
        this.primaryAddress = Objects.requireNonNull(primaryAddress, "primary address is mandatory");
        this.commercialStatus = Objects.requireNonNull(commercialStatus, "commercial status is mandatory");
    }

    /**
     * @return the secondary delivery locations, as an unmodifiable view: the collection is only
     *         changed through the operations of this entity.
     */
    public List<Address> getAdditionalAddresses() {
        return Collections.unmodifiableList(additionalAddresses);
    }

    public void addAdditionalAddress(Address address) {
        Objects.requireNonNull(address, "address is mandatory");
        if (address.equals(primaryAddress) || additionalAddresses.contains(address)) {
            return;
        }
        additionalAddresses.add(address);
    }

    public void removeAdditionalAddress(Address address) {
        additionalAddresses.remove(address);
    }

    /**
     * Promotes a location to primary address. The replaced one is kept as an additional address so
     * no registered location is silently lost.
     */
    public void changePrimaryAddress(Address address) {
        Objects.requireNonNull(address, "primary address is mandatory");
        if (address.equals(primaryAddress)) {
            return;
        }
        additionalAddresses.remove(address);
        Address previous = this.primaryAddress;
        this.primaryAddress = address;
        if (!additionalAddresses.contains(previous)) {
            additionalAddresses.add(previous);
        }
    }

    public void enablePurchases() {
        changeCommercialStatus(BuyerCommercialStatus.ENABLED);
    }

    public void disablePurchases() {
        changeCommercialStatus(BuyerCommercialStatus.DISABLED);
    }

    /**
     * Evaluated before confirming an order: the buyer must be active on the platform and enabled to
     * purchase.
     */
    public boolean canPurchase() {
        return isActive() && commercialStatus.allowsPurchase();
    }

    /**
     * Confirms the cart of this buyer as an order.
     *
     * @param cart            cart of this buyer, which must not be empty.
     * @param orderId         identifier assigned to the new order.
     * @param deliveryAddress delivery location chosen among the registered ones.
     * @param lines           priced lines built from the cart items.
     * @return the order, already in {@code PENDING_PAYMENT}.
     */
    public Order placeOrder(Cart cart, OrderId orderId, Address deliveryAddress, List<OrderLine> lines) {
        Objects.requireNonNull(cart, "cart is mandatory");
        if (!canPurchase()) {
            throw new IllegalStateException("buyer " + getIdentifier() + " is not allowed to purchase");
        }
        if (!cart.belongsTo(this)) {
            throw new IllegalStateException("the cart does not belong to buyer " + getIdentifier());
        }
        if (!knowsAddress(deliveryAddress)) {
            throw new IllegalArgumentException("the delivery address is not registered for this buyer");
        }
        return cart.confirm(orderId, deliveryAddress, lines);
    }

    /**
     * Requests the return of a line of one of the orders of this buyer.
     */
    public ProductReturn requestReturn(ProductReturnId returnId, Order order, OrderLine orderLine, Quantity quantity) {
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.belongsTo(this)) {
            throw new IllegalStateException("the order does not belong to buyer " + getIdentifier());
        }
        return ProductReturn.registerReturn(returnId, order, orderLine, quantity);
    }

    /**
     * @return true when the location is the primary address or one of the additional ones.
     */
    public boolean knowsAddress(Address address) {
        return address != null && (address.equals(primaryAddress) || additionalAddresses.contains(address));
    }

    /**
     * A buyer only operates on their own cart, orders and returns (RD-ROL-04, RD-PED-05).
     */
    @Override
    protected boolean ownsResource(DomainEntity<?> resource) {
        if (resource instanceof Cart cart) {
            return cart.belongsTo(this);
        }
        if (resource instanceof Order order) {
            return order.belongsTo(this);
        }
        return false;
    }

    private void changeCommercialStatus(BuyerCommercialStatus target) {
        if (this.commercialStatus == target) {
            return;
        }
        if (!this.commercialStatus.canTransitionTo(target)) {
            throw new IllegalStateException("buyer cannot move from " + this.commercialStatus + " to " + target);
        }
        this.commercialStatus = target;
    }
}
