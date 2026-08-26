package application.domain.models;

import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import java.util.Objects;
import lombok.Getter;

/**
 * Line of an order: product, variant and quantity committed commercially.
 */
@Getter
public class OrderLine extends DomainEntity<OrderLineId> {

    private final OrderId orderId;

    private final ProductId productId;

    private final ProductVariantId productVariantId;

    private final Quantity quantity;

    private final Money lineAmount;

    public OrderLine(OrderLineId identifier,
                     OrderId orderId,
                     ProductId productId,
                     ProductVariantId productVariantId,
                     Quantity quantity,
                     Money lineAmount) {
        super(identifier);
        this.orderId = Objects.requireNonNull(orderId, "owning order is mandatory");
        this.productId = Objects.requireNonNull(productId, "product is mandatory");
        this.productVariantId = Objects.requireNonNull(productVariantId, "product variant is mandatory");
        Objects.requireNonNull(quantity, "quantity is mandatory");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("an order line cannot be of zero units");
        }
        this.quantity = quantity;
        this.lineAmount = Objects.requireNonNull(lineAmount, "the commercial value of the line is mandatory");
    }

    /**
     * Commits the stock of this line, which produces a reservation movement (RD-INV-04).
     *
     * @throws IllegalArgumentException when the stock record is not the one of this product.
     */
    public InventoryMovement reserveStock(InventoryItem inventoryItem, InventoryMovementId movementId) {
        return requireStockOfThisProduct(inventoryItem).reserve(quantity, movementId);
    }

    /**
     * Frees the stock committed by this line.
     */
    public InventoryMovement releaseStock(InventoryItem inventoryItem, InventoryMovementId movementId) {
        return requireStockOfThisProduct(inventoryItem).releaseReservation(quantity, movementId);
    }

    /**
     * Only physical products generate shipments (RD-LOG-01). The line holds no product type, so the
     * product it refers to decides.
     *
     * @param product the product referenced by this line.
     */
    public boolean requiresShipment(Product product) {
        Objects.requireNonNull(product, "product is mandatory");
        if (!product.getIdentifier().equals(productId)) {
            throw new IllegalArgumentException("the product is not the one referenced by this line");
        }
        return product.requiresInventory();
    }

    public boolean belongsTo(Order order) {
        return order != null && orderId.equals(order.getIdentifier());
    }

    private InventoryItem requireStockOfThisProduct(InventoryItem inventoryItem) {
        Objects.requireNonNull(inventoryItem, "inventory item is mandatory");
        if (!inventoryItem.getProductId().equals(productId)) {
            throw new IllegalArgumentException("the stock record is not the one of product " + productId);
        }
        return inventoryItem;
    }
}
