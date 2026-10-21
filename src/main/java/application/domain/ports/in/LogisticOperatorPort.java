package application.domain.ports.in;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.Product;
import application.domain.models.ProductReturn;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import java.util.Collection;
import java.util.List;

/**
 * Entry contract of the logistics operator role.
 *
 * <p>Runs the physical operation of the warehouses and the dispatches (RD-ROL-06): stock movements,
 * packing, dispatch and delivery confirmation. Only orders holding physical products reach this
 * port (RD-LOG-01).</p>
 */
public interface LogisticOperatorPort {

    // Warehouses and stock

    Warehouse consultWarehouse(User<?> operator, WarehouseId warehouseId);

    List<Warehouse> consultWarehouses(User<?> operator);

    List<InventoryItem> consultStockOfWarehouse(User<?> operator, WarehouseId warehouseId);

    InventoryMovement receiveStock(User<?> operator, InventoryItem inventoryItem, Quantity quantity,
                                   InventoryMovementId movementId);

    InventoryMovement reserveStock(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                   InventoryMovementId movementId);

    InventoryMovement releaseReservation(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                         InventoryMovementId movementId);

    InventoryMovement issueStockForSale(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                        InventoryMovementId movementId);

    InventoryMovement restockReturn(User<?> operator, ProductReturn productReturn, InventoryItem inventoryItem,
                                    InventoryMovementId movementId);

    /**
     * Same restock, entered by identifier: the port resolves the return and the stock record itself
     * so a caller holding only the values of a request never has to reach a repository.
     */
    InventoryMovement restockReturn(User<?> operator, ProductReturnId returnId, InventoryItemId inventoryItemId,
                                    InventoryMovementId movementId);

    // Logistics

    Shipment packShipment(User<?> operator, Shipment shipment, Order order, Collection<Product> products);

    /**
     * Packs by identifier: resolves the order, reuses the shipment when it already exists or opens it
     * otherwise, and collects the products that decide whether the order needs logistics at all
     * (RD-LOG-01).
     *
     * @param originWarehouseId origin of the shipment, mandatory only when it has to be opened.
     * @param deliveryAddress   destination; when null the delivery address of the order is used.
     */
    Shipment packShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId, WarehouseId originWarehouseId,
                          Address deliveryAddress);

    Shipment dispatchShipment(User<?> operator, Shipment shipment, Order order);

    Shipment dispatchShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId);

    Shipment confirmShipmentDelivery(User<?> operator, Shipment shipment, Order order);

    Shipment confirmShipmentDelivery(User<?> operator, ShipmentId shipmentId, OrderId orderId);

    List<Order> consultOrdersByStatus(User<?> operator, OrderStatus status);

    /**
     * @return the shipments of the order; empty while it has not been packed.
     */
    List<Shipment> consultShipmentsOfOrder(User<?> operator, Order order);

    /**
     * Resolves an order by its identifier, which the logistic operations act upon.
     */
    Order consultOrder(User<?> operator, OrderId orderId);

    /**
     * Resolves a stock record by its identifier.
     */
    InventoryItem consultStockRecord(User<?> operator, InventoryItemId inventoryItemId);
}
