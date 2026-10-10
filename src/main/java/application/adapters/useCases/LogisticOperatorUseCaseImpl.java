package application.adapters.useCases;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.Product;
import application.domain.models.ProductReturn;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.LogisticOperatorPort;
import application.domain.services.inventory.ConsultInventoryService;
import application.domain.services.inventory.IssueForSaleService;
import application.domain.services.inventory.ReceiveStockService;
import application.domain.services.inventory.ReleaseReservationService;
import application.domain.services.inventory.ReserveStockService;
import application.domain.services.inventory.RestockReturnService;
import application.domain.services.order.ConsultOrderService;
import application.domain.services.shipment.ConfirmShipmentDeliveryService;
import application.domain.services.shipment.DispatchShipmentService;
import application.domain.services.shipment.PackShipmentService;
import application.domain.services.warehouse.ConsultWarehouseService;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.WarehouseId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implements the logistics operator entry port by delegating to the domain services.
 */
@Service
public class LogisticOperatorUseCaseImpl implements LogisticOperatorPort {

    private final ConsultWarehouseService consultWarehouseService;

    private final ConsultInventoryService consultInventoryService;

    private final ReceiveStockService receiveStockService;

    private final ReserveStockService reserveStockService;

    private final ReleaseReservationService releaseReservationService;

    private final IssueForSaleService issueForSaleService;

    private final RestockReturnService restockReturnService;

    private final PackShipmentService packShipmentService;

    private final DispatchShipmentService dispatchShipmentService;

    private final ConfirmShipmentDeliveryService confirmShipmentDeliveryService;

    private final ConsultOrderService consultOrderService;

    public LogisticOperatorUseCaseImpl(ConsultWarehouseService consultWarehouseService,
                                       ConsultInventoryService consultInventoryService,
                                       ReceiveStockService receiveStockService,
                                       ReserveStockService reserveStockService,
                                       ReleaseReservationService releaseReservationService,
                                       IssueForSaleService issueForSaleService,
                                       RestockReturnService restockReturnService,
                                       PackShipmentService packShipmentService,
                                       DispatchShipmentService dispatchShipmentService,
                                       ConfirmShipmentDeliveryService confirmShipmentDeliveryService,
                                       ConsultOrderService consultOrderService) {
        this.consultWarehouseService = Objects.requireNonNull(consultWarehouseService);
        this.consultInventoryService = Objects.requireNonNull(consultInventoryService);
        this.receiveStockService = Objects.requireNonNull(receiveStockService);
        this.reserveStockService = Objects.requireNonNull(reserveStockService);
        this.releaseReservationService = Objects.requireNonNull(releaseReservationService);
        this.issueForSaleService = Objects.requireNonNull(issueForSaleService);
        this.restockReturnService = Objects.requireNonNull(restockReturnService);
        this.packShipmentService = Objects.requireNonNull(packShipmentService);
        this.dispatchShipmentService = Objects.requireNonNull(dispatchShipmentService);
        this.confirmShipmentDeliveryService = Objects.requireNonNull(confirmShipmentDeliveryService);
        this.consultOrderService = Objects.requireNonNull(consultOrderService);
    }

    @Override
    public Warehouse consultWarehouse(User<?> operator, WarehouseId warehouseId) {
        return consultWarehouseService.consult(operator, warehouseId);
    }

    @Override
    public List<Warehouse> consultWarehouses(User<?> operator) {
        return consultWarehouseService.consultAll(operator);
    }

    @Override
    public List<InventoryItem> consultStockOfWarehouse(User<?> operator, WarehouseId warehouseId) {
        return consultInventoryService.consultByWarehouse(operator, warehouseId);
    }

    @Override
    public InventoryMovement receiveStock(User<?> operator, InventoryItem inventoryItem, Quantity quantity,
                                          InventoryMovementId movementId) {
        return receiveStockService.receive(operator, inventoryItem, quantity, movementId);
    }

    @Override
    public InventoryMovement reserveStock(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                          InventoryMovementId movementId) {
        return reserveStockService.reserve(operator, orderLine, inventoryItem, movementId);
    }

    @Override
    public InventoryMovement releaseReservation(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                                InventoryMovementId movementId) {
        return releaseReservationService.release(operator, orderLine, inventoryItem, movementId);
    }

    @Override
    public InventoryMovement issueStockForSale(User<?> operator, OrderLine orderLine, InventoryItem inventoryItem,
                                               InventoryMovementId movementId) {
        return issueForSaleService.issue(operator, orderLine, inventoryItem, movementId);
    }

    @Override
    public InventoryMovement restockReturn(User<?> operator, ProductReturn productReturn, InventoryItem inventoryItem,
                                           InventoryMovementId movementId) {
        return restockReturnService.restock(operator, productReturn, inventoryItem, movementId);
    }

    @Override
    public Shipment packShipment(User<?> operator, Shipment shipment, Order order, Collection<Product> products) {
        return packShipmentService.pack(operator, shipment, order, products);
    }

    @Override
    public Shipment dispatchShipment(User<?> operator, Shipment shipment, Order order) {
        return dispatchShipmentService.dispatch(operator, shipment, order);
    }

    @Override
    public Shipment confirmShipmentDelivery(User<?> operator, Shipment shipment, Order order) {
        return confirmShipmentDeliveryService.confirmDelivery(operator, shipment, order);
    }

    @Override
    public List<Order> consultOrdersByStatus(User<?> operator, OrderStatus status) {
        return consultOrderService.consultByStatus(operator, status);
    }
}
