package application.domain.support;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.WarehouseId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory doubles of the two inventory ports, kept together because they share the same stored
 * state: a movement always belongs to a stock record.
 */
public final class InMemoryInventoryRepositories {

    private final Map<String, InventoryItem> items = new LinkedHashMap<>();

    private final Map<String, InventoryMovement> movements = new LinkedHashMap<>();

    public InventoryItemRepositoryPort items() {
        return new InventoryItemRepositoryPort() {

            @Override
            public InventoryItem save(InventoryItem inventoryItem) {
                items.put(inventoryItem.getIdentifier().getValue(), inventoryItem);
                return inventoryItem;
            }

            @Override
            public InventoryItem create(InventoryItem inventoryItem) {
                if (items.containsKey(inventoryItem.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + inventoryItem.getIdentifier());
                }
                return save(inventoryItem);
            }

            @Override
            public Optional<InventoryItem> findById(InventoryItemId inventoryItemId) {
                return Optional.ofNullable(items.get(inventoryItemId.getValue()));
            }

            @Override
            public Optional<InventoryItem> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId) {
                return items.values().stream()
                        .filter(item -> item.isFor(productId, warehouseId))
                        .findFirst();
            }

            @Override
            public List<InventoryItem> findByWarehouse(WarehouseId warehouseId) {
                return items.values().stream()
                        .filter(item -> item.getWarehouseId().equals(warehouseId))
                        .toList();
            }

            @Override
            public List<InventoryItem> findByProduct(ProductId productId) {
                return items.values().stream()
                        .filter(item -> item.getProductId().equals(productId))
                        .toList();
            }

            @Override
            public List<InventoryItem> findAll() {
                return new ArrayList<>(items.values());
            }
        };
    }

    public InventoryMovementRepositoryPort movements() {
        return new InventoryMovementRepositoryPort() {

            @Override
            public InventoryMovement save(InventoryMovement inventoryMovement) {
                movements.put(inventoryMovement.getIdentifier().getValue(), inventoryMovement);
                return inventoryMovement;
            }

            @Override
            public Optional<InventoryMovement> findById(InventoryMovementId inventoryMovementId) {
                return Optional.ofNullable(movements.get(inventoryMovementId.getValue()));
            }

            @Override
            public List<InventoryMovement> findByInventoryItem(InventoryItemId inventoryItemId) {
                return movements.values().stream()
                        .filter(movement -> movement.getInventoryItemId().equals(inventoryItemId))
                        .toList();
            }

            @Override
            public List<InventoryMovement> findAll() {
                return new ArrayList<>(movements.values());
            }
        };
    }

    public int storedMovements() {
        return movements.size();
    }
}
