package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.domain.models.Shipment;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;

/**
 * Translates a shipment between its domain shape and its persistence shape. It carries no status of
 * its own (RD-ALC-03), so its public constructor restores it completely.
 */
public final class ShipmentJpaMapper {

    private ShipmentJpaMapper() {
    }

    public static ShipmentJpaEntity toEntity(Shipment shipment) {
        ShipmentJpaEntity entity = new ShipmentJpaEntity();
        entity.setId(shipment.getIdentifier().getValue());
        entity.setOrderId(shipment.getOrderId().getValue());
        entity.setWarehouseId(shipment.getWarehouseId().getValue());
        entity.setDeliveryAddress(shipment.getDeliveryAddress().getValue());
        return entity;
    }

    public static Shipment toDomain(ShipmentJpaEntity entity) {
        return new Shipment(
                ShipmentId.of(entity.getId()),
                OrderId.of(entity.getOrderId()),
                WarehouseId.of(entity.getWarehouseId()),
                Address.of(entity.getDeliveryAddress()));
    }
}
