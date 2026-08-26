package application.domain.models;

import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import java.util.Objects;
import lombok.Getter;

/**
 * Return of products requested by the buyer. After sales process included in the scope of the
 * system.
 *
 * <p>Every return originates in a line of an existing order and reintegrates stock through a return
 * movement (RD-POS-01).</p>
 */
@Getter
public class ProductReturn extends DomainEntity<ProductReturnId> {

    private final OrderId orderId;

    private final OrderLineId orderLineId;

    private final Quantity quantity;

    public ProductReturn(ProductReturnId identifier, OrderId orderId, OrderLineId orderLineId, Quantity quantity) {
        super(identifier);
        this.orderId = Objects.requireNonNull(orderId, "returned order is mandatory");
        this.orderLineId = Objects.requireNonNull(orderLineId, "returned order line is mandatory");
        Objects.requireNonNull(quantity, "quantity is mandatory");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("a return cannot be of zero units");
        }
        this.quantity = quantity;
    }

    /**
     * Registers the return of a line of a delivered order (RD-POS-01).
     *
     * @throws IllegalStateException    when the order has not been delivered yet.
     * @throws IllegalArgumentException when the line does not belong to the order, or the returned
     *                                  quantity exceeds the one of the line.
     */
    public static ProductReturn registerReturn(ProductReturnId identifier,
                                               Order order,
                                               OrderLine orderLine,
                                               Quantity quantity) {
        Objects.requireNonNull(order, "order is mandatory");
        Objects.requireNonNull(orderLine, "order line is mandatory");
        Objects.requireNonNull(quantity, "quantity is mandatory");
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("only a delivered order admits a return");
        }
        if (!orderLine.belongsTo(order)) {
            throw new IllegalArgumentException("the line does not belong to order " + order.getIdentifier());
        }
        if (!orderLine.getQuantity().isGreaterThanOrEqual(quantity)) {
            throw new IllegalArgumentException("the returned quantity exceeds the one of the line");
        }
        return new ProductReturn(identifier, order.getIdentifier(), orderLine.getIdentifier(), quantity);
    }

    /**
     * Reintegrates the stock through a return movement (RD-POS-01).
     */
    public InventoryMovement restock(InventoryItem inventoryItem, InventoryMovementId movementId) {
        Objects.requireNonNull(inventoryItem, "inventory item is mandatory");
        return inventoryItem.returnStock(quantity, movementId);
    }

    public boolean isFor(Order order) {
        return order != null && orderId.equals(order.getIdentifier());
    }
}
