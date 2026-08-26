package application.domain.models;

import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.WarehouseId;
import application.domain.valuesObjects.WarehouseType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;

/**
 * Place where the physical inventory is managed. Controls the physical storage spaces.
 *
 * <p>Inventory is distributed: every stock record is bound to one product and one specific
 * warehouse, and there is no global stock (RD-INV-01). The warehouse therefore holds its own stock
 * records, which are the ones its operations act upon.</p>
 */
@Getter
public abstract class Warehouse extends DomainEntity<WarehouseId> {

    private final WarehouseType type;

    private final List<InventoryItem> inventoryItems = new ArrayList<>();

    protected Warehouse(WarehouseId identifier, WarehouseType type) {
        super(identifier);
        this.type = Objects.requireNonNull(type, "warehouse type is mandatory");
    }

    /**
     * @return the stock records held here, as an unmodifiable view.
     */
    public List<InventoryItem> getInventoryItems() {
        return Collections.unmodifiableList(inventoryItems);
    }

    /**
     * Binds a stock record to this warehouse.
     *
     * @throws IllegalArgumentException when the record belongs to another warehouse, or when the
     *                                  product already has stock here: the pair
     *                                  (productId, warehouseId) is unique (RD-ID-05).
     */
    public void addInventoryItem(InventoryItem inventoryItem) {
        Objects.requireNonNull(inventoryItem, "inventory item is mandatory");
        if (!inventoryItem.getWarehouseId().equals(getIdentifier())) {
            throw new IllegalArgumentException("the inventory item belongs to another warehouse");
        }
        if (stockOf(inventoryItem.getProductId()).isPresent()) {
            throw new IllegalArgumentException(
                    "product " + inventoryItem.getProductId() + " already has stock in warehouse " + getIdentifier());
        }
        inventoryItems.add(inventoryItem);
    }

    /**
     * @return the stock of a product in this warehouse, empty when the product has none here.
     */
    public Optional<InventoryItem> stockOf(ProductId productId) {
        return inventoryItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Registers stock into the warehouse, which produces an inbound movement (RD-INV-04).
     */
    public InventoryMovement receive(ProductId productId, Quantity quantity, InventoryMovementId movementId) {
        return requireStockOf(productId).receive(quantity, movementId);
    }

    /**
     * Takes the stock committed to an order line physically out of the warehouse, which produces a
     * sale outbound movement (RD-INV-04, RD-LOG-02).
     */
    public InventoryMovement dispatch(OrderLine orderLine, InventoryMovementId movementId) {
        Objects.requireNonNull(orderLine, "order line is mandatory");
        return requireStockOf(orderLine.getProductId()).issueForSale(orderLine.getQuantity(), movementId);
    }

    private InventoryItem requireStockOf(ProductId productId) {
        return stockOf(productId).orElseThrow(() -> new IllegalStateException(
                "product " + productId + " has no stock record in warehouse " + getIdentifier()));
    }
}
