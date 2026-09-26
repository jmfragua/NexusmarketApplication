package application.domain.ports.out;

import application.domain.models.InventoryItem;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the distributed stock. A stock record is bound to one product and one specific
 * warehouse, and the pair is unique (RD-INV-01, RD-ID-05).
 */
public interface InventoryItemRepositoryPort {

    InventoryItem save(InventoryItem inventoryItem);

    Optional<InventoryItem> findById(InventoryItemId inventoryItemId);

    Optional<InventoryItem> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId);

    List<InventoryItem> findByWarehouse(WarehouseId warehouseId);

    List<InventoryItem> findByProduct(ProductId productId);

    List<InventoryItem> findAll();
}
