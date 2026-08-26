package application.domain.models;

import application.domain.valuesObjects.WarehouseId;
import application.domain.valuesObjects.WarehouseType;

/**
 * Warehouse owned by the marketplace and operated directly by the platform. It is not tied to any
 * seller.
 */
public class MarketplaceWarehouse extends Warehouse {

    public MarketplaceWarehouse(WarehouseId identifier) {
        super(identifier, WarehouseType.MARKETPLACE);
    }
}
