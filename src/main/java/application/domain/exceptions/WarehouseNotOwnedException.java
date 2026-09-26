package application.domain.exceptions;

import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;

/**
 * Raised when a seller tries to operate on a warehouse they do not own (RG-03, RD-ROL-03).
 */
public class WarehouseNotOwnedException extends DomainException {

    public WarehouseNotOwnedException(SellerId sellerId, WarehouseId warehouseId) {
        super("warehouse " + warehouseId + " does not belong to seller " + sellerId);
    }
}
