package application.domain.models;

import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.InventoryMovementType;
import application.domain.valuesObjects.Quantity;
import java.util.Objects;
import lombok.Getter;

/**
 * Record of a stock change. Guarantees the traceability of the distributed inventory: no stock
 * change happens without a movement of one of the five defined types (RD-INV-04).
 */
@Getter
public class InventoryMovement extends DomainEntity<InventoryMovementId> {

    private final InventoryItemId inventoryItemId;

    private final InventoryMovementType movementType;

    private final Quantity quantity;

    public InventoryMovement(InventoryMovementId identifier,
                             InventoryItemId inventoryItemId,
                             InventoryMovementType movementType,
                             Quantity quantity) {
        super(identifier);
        this.inventoryItemId = Objects.requireNonNull(inventoryItemId, "affected stock record is mandatory");
        this.movementType = Objects.requireNonNull(movementType, "movement type is mandatory");
        this.quantity = Objects.requireNonNull(quantity, "quantity is mandatory");
    }

    /**
     * @return true for inbound and return movements.
     */
    public boolean isInbound() {
        return movementType.isInbound();
    }

    /**
     * @return true for sale outbound movements.
     */
    public boolean isOutbound() {
        return movementType.isOutbound();
    }

    public boolean appliesTo(InventoryItem inventoryItem) {
        return inventoryItem != null && inventoryItemId.equals(inventoryItem.getIdentifier());
    }
}
