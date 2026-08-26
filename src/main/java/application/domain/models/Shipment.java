package application.domain.models;

import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import java.util.Objects;
import lombok.Getter;

/**
 * Logistic process for physical products: packing, dispatch and transport of the order.
 *
 * <p>Every shipment leaves from a specific warehouse, the one supporting the stock exit
 * (RD-LOG-02). The business specification declares no state catalogue of its own for the shipment:
 * the logistic progress is reflected on the state of the order, so these operations move the order
 * forward instead of holding a state here (RD-ALC-03).</p>
 */
@Getter
public class Shipment extends DomainEntity<ShipmentId> {

    private final OrderId orderId;

    private final WarehouseId warehouseId;

    private final Address deliveryAddress;

    public Shipment(ShipmentId identifier, OrderId orderId, WarehouseId warehouseId, Address deliveryAddress) {
        super(identifier);
        this.orderId = Objects.requireNonNull(orderId, "dispatched order is mandatory");
        this.warehouseId = Objects.requireNonNull(warehouseId, "origin warehouse is mandatory");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "delivery address is mandatory");
    }

    /**
     * Prepares the packing of the order, which requires a validated payment (RD-PED-03).
     */
    public void pack(Order order) {
        requireOwnOrder(order);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("only a paid order can be packed");
        }
    }

    /**
     * Registers the physical exit from the warehouse, which moves the order to
     * {@code DISPATCHED}.
     */
    public void dispatch(Order order) {
        requireOwnOrder(order).dispatch();
    }

    /**
     * Confirms the delivery, which closes the order at {@code DELIVERED}.
     */
    public void confirmDelivery(Order order) {
        requireOwnOrder(order).completeDelivery();
    }

    /**
     * @return true when the shipment leaves from the given warehouse.
     */
    public boolean departsFrom(Warehouse warehouse) {
        return warehouse != null && warehouseId.equals(warehouse.getIdentifier());
    }

    private Order requireOwnOrder(Order order) {
        Objects.requireNonNull(order, "order is mandatory");
        if (!orderId.equals(order.getIdentifier())) {
            throw new IllegalArgumentException("the order is not the one of this shipment");
        }
        return order;
    }
}
