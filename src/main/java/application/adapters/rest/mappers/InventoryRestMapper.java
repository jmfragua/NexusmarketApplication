package application.adapters.rest.mappers;

import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;

/**
 * Translates stock records, movements and warehouses into their REST shapes.
 */
public final class InventoryRestMapper {

    private InventoryRestMapper() {
    }

    public static InventoryItemResponse toResponse(InventoryItem inventoryItem) {
        return new InventoryItemResponse(
                inventoryItem.getIdentifier().getValue(),
                inventoryItem.getProductId().getValue(),
                inventoryItem.getWarehouseId().getValue(),
                inventoryItem.getAvailableQuantity().getValue(),
                inventoryItem.getReservedQuantity().getValue(),
                inventoryItem.getCondition().code());
    }

    public static InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getIdentifier().getValue(),
                movement.getInventoryItemId().getValue(),
                movement.getMovementType().code(),
                movement.getQuantity().getValue());
    }

    public static WarehouseResponse toResponse(Warehouse warehouse) {
        String sellerId = warehouse instanceof SellerWarehouse sellerWarehouse
                ? sellerWarehouse.getSellerId().getValue()
                : null;
        return new WarehouseResponse(warehouse.getIdentifier().getValue(), warehouse.getType().code(), sellerId);
    }
}
