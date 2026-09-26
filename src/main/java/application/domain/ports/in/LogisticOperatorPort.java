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
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.Quantity;
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

    // Logistics

    Shipment packShipment(User<?> operator, Shipment shipment, Order order, Collection<Product> products);

    Shipment dispatchShipment(User<?> operator, Shipment shipment, Order order);

    Shipment confirmShipmentDelivery(User<?> operator, Shipment shipment, Order order);

    List<Order> consultOrdersByStatus(User<?> operator, OrderStatus status);
}
