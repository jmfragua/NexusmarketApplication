package application.domain.ports.out;

import application.domain.models.Shipment;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the logistics. Only orders holding physical products generate shipments, and every
 * shipment departs from a concrete warehouse (RD-LOG-01, RD-LOG-02).
 */
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(ShipmentId shipmentId);

    List<Shipment> findByOrder(OrderId orderId);

    List<Shipment> findByWarehouse(WarehouseId warehouseId);

    List<Shipment> findAll();
}
