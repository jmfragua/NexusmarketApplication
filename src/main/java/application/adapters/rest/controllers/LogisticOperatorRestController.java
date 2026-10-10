package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.ShipmentActionRequest;
import application.adapters.rest.dtos.requests.StockLineMovementRequest;
import application.adapters.rest.dtos.requests.StockMovementRequest;
import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
import application.adapters.rest.dtos.responses.OrderResponse;
import application.adapters.rest.dtos.responses.ShipmentResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.InventoryItem;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.Product;
import application.domain.models.ProductReturn;
import application.domain.models.Shipment;
import application.domain.ports.in.LogisticOperatorPort;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import application.infrastructure.security.DomainUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logistics operator endpoints: warehouse stock movements and the shipment lifecycle.
 *
 * <p>As in the seller controller, output ports are used only to resolve entities from the
 * identifiers in the request; the entry port and its domain services remain the only place where
 * anything is decided or validated (RD-ROL-06).</p>
 */
@RestController
@RequestMapping("/api/v1/logistics")
public class LogisticOperatorRestController {

    private final LogisticOperatorPort logisticOperatorPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final ProductRepositoryPort productRepositoryPort;

    private final ProductReturnRepositoryPort productReturnRepositoryPort;

    public LogisticOperatorRestController(LogisticOperatorPort logisticOperatorPort,
                                          OrderRepositoryPort orderRepositoryPort,
                                          InventoryItemRepositoryPort inventoryItemRepositoryPort,
                                          ShipmentRepositoryPort shipmentRepositoryPort,
                                          ProductRepositoryPort productRepositoryPort,
                                          ProductReturnRepositoryPort productReturnRepositoryPort) {
        this.logisticOperatorPort = Objects.requireNonNull(logisticOperatorPort);
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort);
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort);
        this.shipmentRepositoryPort = Objects.requireNonNull(shipmentRepositoryPort);
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort);
        this.productReturnRepositoryPort = Objects.requireNonNull(productReturnRepositoryPort);
    }

    // Warehouses and stock

    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseResponse>> consultWarehouses(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(logisticOperatorPort.consultWarehouses(principal.user()).stream()
                .map(InventoryRestMapper::toResponse)
                .toList());
    }

    @GetMapping("/warehouses/{warehouseId}/stock")
    public ResponseEntity<List<InventoryItemResponse>> consultStockOfWarehouse(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String warehouseId) {
        return ResponseEntity.ok(
                logisticOperatorPort.consultStockOfWarehouse(principal.user(), WarehouseId.of(warehouseId)).stream()
                        .map(InventoryRestMapper::toResponse)
                        .toList());
    }

    @PostMapping("/stock/receipts")
    public ResponseEntity<InventoryMovementResponse> receiveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        InventoryMovementResponse response = InventoryRestMapper.toResponse(logisticOperatorPort.receiveStock(
                principal.user(), stockOf(request.inventoryItemId()), Quantity.of(request.quantity()),
                InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/reservations")
    public ResponseEntity<InventoryMovementResponse> reserveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.reserveStock(principal.user(), lineOf(request),
                        stockOf(request.inventoryItemId()), InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/releases")
    public ResponseEntity<InventoryMovementResponse> releaseReservation(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.releaseReservation(principal.user(), lineOf(request),
                        stockOf(request.inventoryItemId()), InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/sale-outbounds")
    public ResponseEntity<InventoryMovementResponse> issueStockForSale(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.issueStockForSale(principal.user(), lineOf(request),
                        stockOf(request.inventoryItemId()), InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/returns/{returnId}")
    public ResponseEntity<InventoryMovementResponse> restockReturn(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String returnId,
            @RequestParam String inventoryItemId,
            @RequestParam String movementId) {
        ProductReturn productReturn = productReturnRepositoryPort.findById(ProductReturnId.of(returnId))
                .orElseThrow(() -> new EntityNotFoundException("product return", ProductReturnId.of(returnId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.restockReturn(principal.user(), productReturn, stockOf(inventoryItemId),
                        InventoryMovementId.of(movementId))));
    }

    // Shipments

    @PostMapping("/shipments/packaging")
    public ResponseEntity<ShipmentResponse> packShipment(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                         @Valid @RequestBody ShipmentActionRequest request) {
        Order order = orderOf(request.orderId());
        Shipment shipment = shipmentRepositoryPort.findById(ShipmentId.of(request.shipmentId()))
                .orElseGet(() -> new Shipment(
                        ShipmentId.of(request.shipmentId()),
                        order.getIdentifier(),
                        WarehouseId.of(Objects.requireNonNull(request.warehouseId(),
                                "the origin warehouse is mandatory to create a shipment")),
                        Address.of(Objects.requireNonNullElse(request.deliveryAddress(),
                                order.getDeliveryAddress().getValue()))));
        List<Product> products = productsOf(order);
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(
                logisticOperatorPort.packShipment(principal.user(), shipment, order, products)));
    }

    @PostMapping("/shipments/dispatches")
    public ResponseEntity<ShipmentResponse> dispatchShipment(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                             @Valid @RequestBody ShipmentActionRequest request) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(logisticOperatorPort.dispatchShipment(
                principal.user(), shipmentOf(request.shipmentId()), orderOf(request.orderId()))));
    }

    @PostMapping("/shipments/deliveries")
    public ResponseEntity<ShipmentResponse> confirmDelivery(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                            @Valid @RequestBody ShipmentActionRequest request) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(logisticOperatorPort.confirmShipmentDelivery(
                principal.user(), shipmentOf(request.shipmentId()), orderOf(request.orderId()))));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> consultOrdersByStatus(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam String status) {
        return ResponseEntity.ok(
                logisticOperatorPort.consultOrdersByStatus(principal.user(), OrderStatus.valueOf(status)).stream()
                        .map(OrderRestMapper::toResponse)
                        .toList());
    }

    /**
     * Collects the products referenced by the order, which are what decide whether it needs
     * logistics at all: the lines carry no product type (RD-LOG-01).
     */
    private List<Product> productsOf(Order order) {
        return order.getLines().stream()
                .map(line -> productRepositoryPort.findById(line.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("product", line.getProductId())))
                .toList();
    }

    private Order orderOf(String orderId) {
        return orderRepositoryPort.findById(OrderId.of(orderId))
                .orElseThrow(() -> new EntityNotFoundException("order", OrderId.of(orderId)));
    }

    private Shipment shipmentOf(String shipmentId) {
        return shipmentRepositoryPort.findById(ShipmentId.of(shipmentId))
                .orElseThrow(() -> new EntityNotFoundException("shipment", ShipmentId.of(shipmentId)));
    }

    private InventoryItem stockOf(String inventoryItemId) {
        return inventoryItemRepositoryPort.findById(InventoryItemId.of(inventoryItemId))
                .orElseThrow(() -> new EntityNotFoundException("stock record",
                        InventoryItemId.of(inventoryItemId)));
    }

    private OrderLine lineOf(StockLineMovementRequest request) {
        Order order = orderOf(request.orderId());
        return order.getLines().stream()
                .filter(line -> line.getIdentifier().getValue().equals(request.orderLineId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "order line " + request.orderLineId() + " does not belong to order " + request.orderId()));
    }
}
