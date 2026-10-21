package application.domain.exceptions;

import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.WarehouseId;

/**
 * Raised when a second stock record is opened for the same product in the same warehouse: the pair
 * (productId, warehouseId) identifies a stock record uniquely (RD-ID-05, RD-INV-01). Also raised when
 * the identifier supplied for a new record is already taken, which would otherwise overwrite the
 * existing record (RD-ID-01).
 */
public class DuplicateInventoryItemException extends DomainException {

    public DuplicateInventoryItemException(ProductId productId, WarehouseId warehouseId) {
        super("product " + productId + " already holds a stock record in warehouse " + warehouseId);
    }

    public DuplicateInventoryItemException(InventoryItemId inventoryItemId) {
        super("a stock record with identifier " + inventoryItemId + " already exists");
    }
}
