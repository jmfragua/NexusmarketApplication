package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import application.domain.valuesObjects.WarehouseType;

/**
 * Translates a warehouse between its domain shape and its persistence shape.
 *
 * <p>Both specializations restore through their public constructor: the warehouse type is fixed at
 * creation and never changes (RD-VO-14), so there is nothing a reload could falsify.</p>
 */
public final class WarehouseJpaMapper {

    private WarehouseJpaMapper() {
    }

    public static WarehouseJpaEntity toEntity(Warehouse warehouse) {
        WarehouseJpaEntity entity = new WarehouseJpaEntity();
        entity.setId(warehouse.getIdentifier().getValue());
        entity.setWarehouseType(warehouse.getType().code());
        if (warehouse instanceof SellerWarehouse sellerWarehouse) {
            entity.setSellerId(sellerWarehouse.getSellerId().getValue());
        }
        return entity;
    }

    public static Warehouse toDomain(WarehouseJpaEntity entity) {
        WarehouseId identifier = WarehouseId.of(entity.getId());
        return WarehouseType.valueOf(entity.getWarehouseType()) == WarehouseType.SELLER
                ? new SellerWarehouse(identifier, SellerId.of(entity.getSellerId()))
                : new MarketplaceWarehouse(identifier);
    }

    public static SellerWarehouse toSellerWarehouseDomain(WarehouseJpaEntity entity) {
        return new SellerWarehouse(WarehouseId.of(entity.getId()), SellerId.of(entity.getSellerId()));
    }
}
