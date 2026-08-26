package application.domain.models;

import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.InventoryMovementType;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.WarehouseId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Stock available for sale. Inventory is distributed, so a stock record is always bound to one
 * product and one specific warehouse (RD-INV-01): the pair (productId, warehouseId) is unique
 * (RD-ID-05).
 *
 * <p>Negative stock is never allowed (RD-INV-02), stock that does not exist or is marked as damaged
 * can never be reserved (RD-INV-03), and every stock change is materialized as an
 * {@link InventoryMovement}: there is no direct quantity change without an associated movement
 * (RD-INV-04).</p>
 */
@Getter
public class InventoryItem extends DomainEntity<InventoryItemId> {

    private final ProductId productId;

    private final WarehouseId warehouseId;

    private final List<InventoryMovement> movements = new ArrayList<>();

    private Quantity availableQuantity;

    private Quantity reservedQuantity;

    private StockCondition condition;

    public InventoryItem(InventoryItemId identifier, ProductId productId, WarehouseId warehouseId) {
        super(identifier);
        this.productId = Objects.requireNonNull(productId, "product is mandatory");
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouse is mandatory");
        this.availableQuantity = Quantity.zero();
        this.reservedQuantity = Quantity.zero();
        this.condition = StockCondition.AVAILABLE;
    }

    /**
     * @return the traceability of the stock, as an unmodifiable view.
     */
    public List<InventoryMovement> getMovements() {
        return Collections.unmodifiableList(movements);
    }

    /**
     * Registers stock into the warehouse.
     */
    public InventoryMovement receive(Quantity quantity, InventoryMovementId movementId) {
        requirePositive(quantity);
        InventoryMovement movement = register(InventoryMovementType.INBOUND, quantity, movementId);
        this.availableQuantity = this.availableQuantity.add(quantity);
        return movement;
    }

    /**
     * Commits stock to an order.
     *
     * @throws IllegalStateException when there is not enough stock or the stock is damaged
     *                               (RD-INV-03).
     */
    public InventoryMovement reserve(Quantity quantity, InventoryMovementId movementId) {
        requirePositive(quantity);
        if (!canReserve(quantity)) {
            throw new IllegalStateException(
                    "stock of product " + productId + " cannot be reserved: condition " + condition
                            + ", available " + availableQuantity);
        }
        InventoryMovement movement = register(InventoryMovementType.RESERVATION, quantity, movementId);
        this.availableQuantity = this.availableQuantity.subtract(quantity);
        this.reservedQuantity = this.reservedQuantity.add(quantity);
        return movement;
    }

    /**
     * Frees stock that had been committed and returns it to the available balance. It is recorded
     * as an adjustment, the movement type the business defines to correct the registered stock.
     */
    public InventoryMovement releaseReservation(Quantity quantity, InventoryMovementId movementId) {
        requirePositive(quantity);
        if (!reservedQuantity.isGreaterThanOrEqual(quantity)) {
            throw new IllegalStateException(
                    "there is not enough reserved stock to release: reserved " + reservedQuantity + ", requested " + quantity);
        }
        InventoryMovement movement = register(InventoryMovementType.ADJUSTMENT, quantity, movementId);
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        this.availableQuantity = this.availableQuantity.add(quantity);
        return movement;
    }

    /**
     * Takes the committed stock physically out of the warehouse because the order was dispatched.
     */
    public InventoryMovement issueForSale(Quantity quantity, InventoryMovementId movementId) {
        requirePositive(quantity);
        if (!reservedQuantity.isGreaterThanOrEqual(quantity)) {
            throw new IllegalStateException(
                    "there is not enough reserved stock to issue: reserved " + reservedQuantity + ", requested " + quantity);
        }
        InventoryMovement movement = register(InventoryMovementType.SALE_OUTBOUND, quantity, movementId);
        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        return movement;
    }

    /**
     * Corrects the registered available stock. The balance never becomes negative because
     * {@link Quantity} makes a negative value impossible to build (RD-INV-02).
     *
     * <p>The business specification names this operation with a reason, but the movement declares no
     * attribute able to hold it, so no unmodelled field is added (RD-ALC-03).</p>
     *
     * @param newAvailableQuantity corrected available stock.
     */
    public InventoryMovement adjust(Quantity newAvailableQuantity, InventoryMovementId movementId) {
        Objects.requireNonNull(newAvailableQuantity, "quantity is mandatory");
        Quantity difference = newAvailableQuantity.isGreaterThanOrEqual(availableQuantity)
                ? newAvailableQuantity.subtract(availableQuantity)
                : availableQuantity.subtract(newAvailableQuantity);
        InventoryMovement movement = register(InventoryMovementType.ADJUSTMENT, difference, movementId);
        this.availableQuantity = newAvailableQuantity;
        return movement;
    }

    /**
     * Reintegrates stock coming from a product return (RD-POS-01).
     */
    public InventoryMovement returnStock(Quantity quantity, InventoryMovementId movementId) {
        requirePositive(quantity);
        InventoryMovement movement = register(InventoryMovementType.RETURN, quantity, movementId);
        this.availableQuantity = this.availableQuantity.add(quantity);
        return movement;
    }

    /**
     * Marks the stock as damaged, which excludes it from every reservation (RD-INV-03).
     */
    public void markAsDamaged() {
        if (!condition.canTransitionTo(StockCondition.DAMAGED)) {
            throw new IllegalStateException("stock cannot move from " + condition + " to " + StockCondition.DAMAGED);
        }
        this.condition = StockCondition.DAMAGED;
    }

    /**
     * Stock that does not exist or is marked as damaged can never be reserved (RD-INV-03).
     */
    public boolean canReserve(Quantity quantity) {
        return quantity != null
                && quantity.isPositive()
                && condition.allowsReservation()
                && availableQuantity.isGreaterThanOrEqual(quantity);
    }

    /**
     * @return true when this stock record is the one of the given product and warehouse.
     */
    public boolean isFor(ProductId product, WarehouseId warehouse) {
        return productId.equals(product) && warehouseId.equals(warehouse);
    }

    private InventoryMovement register(InventoryMovementType type, Quantity quantity, InventoryMovementId movementId) {
        InventoryMovement movement = new InventoryMovement(movementId, getIdentifier(), type, quantity);
        movements.add(movement);
        return movement;
    }

    private static void requirePositive(Quantity quantity) {
        Objects.requireNonNull(quantity, "quantity is mandatory");
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("an inventory movement cannot be of zero units");
        }
    }
}
