package application.adapters.useCases;

import application.domain.exceptions.EntityNotFoundException;
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
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
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
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.UserRole;
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

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final OrderRepositoryPort orderRepositoryPort;

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final ProductRepositoryPort productRepositoryPort;

    private final ProductReturnRepositoryPort productReturnRepositoryPort;

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
                                       ConsultOrderService consultOrderService,
                                       ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                       OrderRepositoryPort orderRepositoryPort,
                                       InventoryItemRepositoryPort inventoryItemRepositoryPort,
                                       ShipmentRepositoryPort shipmentRepositoryPort,
                                       ProductRepositoryPort productRepositoryPort,
                                       ProductReturnRepositoryPort productReturnRepositoryPort) {
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
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService);
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort);
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort);
        this.shipmentRepositoryPort = Objects.requireNonNull(shipmentRepositoryPort);
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort);
        this.productReturnRepositoryPort = Objects.requireNonNull(productReturnRepositoryPort);
    }

    /**
     * The roles that take part in the management of an order are the ones allowed to resolve one
     * here, mirroring {@code ConsultOrderService.consultByStatus} (RD-ROL-06).
     */
    @Override
    public Order consultOrder(User<?> operator, OrderId orderId) {
        validateCanResolve(operator, "consult order");
        return requireOrder(orderId);
    }

    @Override
    public InventoryItem consultStockRecord(User<?> operator, InventoryItemId inventoryItemId) {
        validateCanResolve(operator, "consult stock record");
        return requireStockRecord(inventoryItemId);
    }

    /**
     * Guards every resolution by identifier, and it runs <em>before</em> the lookup on purpose: if it
     * ran after, an actor with no right to the operation would learn from a 404 versus a 403 whether
     * the entity exists (RD-ROL-03). The role check has to be the first thing that happens.
     */
    private void validateCanResolve(User<?> operator, String operation) {
        validateRoleAuthorizationService.validate(operator, operation, UserRole.SELLER,
                UserRole.LOGISTICS_OPERATOR, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
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
    public InventoryMovement restockReturn(User<?> operator, ProductReturnId returnId,
                                           InventoryItemId inventoryItemId, InventoryMovementId movementId) {
        validateCanResolve(operator, "restock return");
        ProductReturn productReturn = productReturnRepositoryPort.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("product return", returnId));
        return restockReturn(operator, productReturn, requireStockRecord(inventoryItemId), movementId);
    }

    @Override
    public List<Order> consultOrdersByStatus(User<?> operator, OrderStatus status) {
        return consultOrderService.consultByStatus(operator, status);
    }

    @Override
    public Shipment packShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId,
                                 WarehouseId originWarehouseId, Address deliveryAddress) {
        validateCanResolve(operator, "pack shipment");
        Order order = requireOrder(orderId);
        Shipment shipment = shipmentRepositoryPort.findById(shipmentId)
                .orElseGet(() -> new Shipment(
                        shipmentId,
                        order.getIdentifier(),
                        Objects.requireNonNull(originWarehouseId,
                                "the origin warehouse is mandatory to open a shipment"),
                        Objects.requireNonNullElse(deliveryAddress, order.getDeliveryAddress())));
        return packShipment(operator, shipment, order, productsOf(order));
    }

    @Override
    public Shipment dispatchShipment(User<?> operator, ShipmentId shipmentId, OrderId orderId) {
        validateCanResolve(operator, "dispatch shipment");
        return dispatchShipment(operator, requireShipment(shipmentId), requireOrder(orderId));
    }

    @Override
    public Shipment confirmShipmentDelivery(User<?> operator, ShipmentId shipmentId, OrderId orderId) {
        validateCanResolve(operator, "confirm shipment delivery");
        return confirmShipmentDelivery(operator, requireShipment(shipmentId), requireOrder(orderId));
    }

    /**
     * Collects the products referenced by the order, which are what decide whether it needs logistics
     * at all: the lines carry no product type (RD-LOG-01).
     */
    private List<Product> productsOf(Order order) {
        return order.getLines().stream()
                .map(line -> productRepositoryPort.findById(line.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("product", line.getProductId())))
                .toList();
    }

    private Order requireOrder(OrderId orderId) {
        return orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order", orderId));
    }

    private Shipment requireShipment(ShipmentId shipmentId) {
        return shipmentRepositoryPort.findById(shipmentId)
                .orElseThrow(() -> new EntityNotFoundException("shipment", shipmentId));
    }

    private InventoryItem requireStockRecord(InventoryItemId inventoryItemId) {
        return inventoryItemRepositoryPort.findById(inventoryItemId)
                .orElseThrow(() -> new EntityNotFoundException("stock record", inventoryItemId));
    }
}
