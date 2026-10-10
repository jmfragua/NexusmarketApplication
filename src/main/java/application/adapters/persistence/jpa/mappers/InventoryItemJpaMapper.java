package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.InventoryItemJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.InventoryMovementType;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;

/**
 * Translates a stock record between its domain shape and its persistence shape.
 *
 * <p>Balances, condition and history are restored verbatim through
 * {@link InventoryItem#reconstruct}: replaying {@code receive}/{@code reserve} would fabricate
 * duplicated movements that never happened, breaking the traceability rule (RD-INV-04,
 * RD-ALC-04).</p>
 */
public final class InventoryItemJpaMapper {

    private InventoryItemJpaMapper() {
    }

    public static InventoryItemJpaEntity toEntity(InventoryItem inventoryItem) {
        InventoryItemJpaEntity entity = new InventoryItemJpaEntity();
        entity.setId(inventoryItem.getIdentifier().getValue());
        entity.setProductId(inventoryItem.getProductId().getValue());
        entity.setWarehouseId(inventoryItem.getWarehouseId().getValue());
        entity.setAvailableQuantity(inventoryItem.getAvailableQuantity().getValue());
        entity.setReservedQuantity(inventoryItem.getReservedQuantity().getValue());
        entity.setCondition(inventoryItem.getCondition().code());
        inventoryItem.getMovements()
                .forEach(movement -> entity.getMovements().add(toMovementEntity(movement, entity)));
        return entity;
    }

    public static InventoryItem toDomain(InventoryItemJpaEntity entity) {
        List<InventoryMovement> movements = entity.getMovements().stream()
                .map(InventoryItemJpaMapper::toMovementDomain)
                .toList();
        return InventoryItem.reconstruct(
                InventoryItemId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                WarehouseId.of(entity.getWarehouseId()),
                Quantity.of(entity.getAvailableQuantity()),
                Quantity.of(entity.getReservedQuantity()),
                StockCondition.valueOf(entity.getCondition()),
                movements);
    }

    public static InventoryMovementJpaEntity toMovementEntity(InventoryMovement movement,
                                                              InventoryItemJpaEntity inventoryItem) {
        InventoryMovementJpaEntity entity = new InventoryMovementJpaEntity();
        entity.setId(movement.getIdentifier().getValue());
        entity.setInventoryItem(inventoryItem);
        entity.setMovementType(movement.getMovementType().code());
        entity.setQuantity(movement.getQuantity().getValue());
        return entity;
    }

    public static InventoryMovement toMovementDomain(InventoryMovementJpaEntity entity) {
        return new InventoryMovement(
                InventoryMovementId.of(entity.getId()),
                InventoryItemId.of(entity.getInventoryItem().getId()),
                InventoryMovementType.valueOf(entity.getMovementType()),
                Quantity.of(entity.getQuantity()));
    }
}
