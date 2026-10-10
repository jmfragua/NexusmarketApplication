package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.ShipmentActionRequest;
import application.adapters.rest.dtos.requests.StockLineMovementRequest;
import application.adapters.rest.dtos.requests.StockMovementRequest;
import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
import application.adapters.rest.dtos.responses.OrderResponse;
import application.adapters.rest.dtos.responses.PageResponse;
import application.adapters.rest.dtos.responses.ShipmentResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.InventoryItem;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.ports.in.LogisticOperatorPort;
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
 * <p>As in the seller controller, the identifiers in the request are resolved through the entry
 * port, which validates the role while loading. The controller only translates HTTP to the port and
 * back: nothing is decided or looked up here (RD-ROL-06).</p>
 */
@RestController
@RequestMapping("/api/v1/logistics")
public class LogisticOperatorRestController {

    private final LogisticOperatorPort logisticOperatorPort;

    public LogisticOperatorRestController(LogisticOperatorPort logisticOperatorPort) {
        this.logisticOperatorPort = Objects.requireNonNull(logisticOperatorPort);
    }

    // Warehouses and stock

    @GetMapping("/warehouses")
    public ResponseEntity<PageResponse<WarehouseResponse>> consultWarehouses(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(logisticOperatorPort.consultWarehouses(principal.user()).stream()
                .map(InventoryRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/warehouses/{warehouseId}/stock")
    public ResponseEntity<PageResponse<InventoryItemResponse>> consultStockOfWarehouse(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(
                logisticOperatorPort.consultStockOfWarehouse(principal.user(), WarehouseId.of(warehouseId)).stream()
                        .map(InventoryRestMapper::toResponse)
                        .toList(), page, size));
    }

    @PostMapping("/stock/receipts")
    public ResponseEntity<InventoryMovementResponse> receiveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        InventoryMovementResponse response = InventoryRestMapper.toResponse(logisticOperatorPort.receiveStock(
                principal.user(), stockOf(principal, request.inventoryItemId()),
                Quantity.of(request.quantity()), InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/reservations")
    public ResponseEntity<InventoryMovementResponse> reserveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.reserveStock(principal.user(), lineOf(principal, request),
                        stockOf(principal, request.inventoryItemId()),
                        InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/releases")
    public ResponseEntity<InventoryMovementResponse> releaseReservation(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.releaseReservation(principal.user(), lineOf(principal, request),
                        stockOf(principal, request.inventoryItemId()),
                        InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/sale-outbounds")
    public ResponseEntity<InventoryMovementResponse> issueStockForSale(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockLineMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.issueStockForSale(principal.user(), lineOf(principal, request),
                        stockOf(principal, request.inventoryItemId()),
                        InventoryMovementId.of(request.movementId()))));
    }

    @PostMapping("/stock/returns/{returnId}")
    public ResponseEntity<InventoryMovementResponse> restockReturn(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String returnId,
            @RequestParam String inventoryItemId,
            @RequestParam String movementId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                logisticOperatorPort.restockReturn(principal.user(), ProductReturnId.of(returnId),
                        InventoryItemId.of(inventoryItemId), InventoryMovementId.of(movementId))));
    }

    // Shipments

    @PostMapping("/shipments/packaging")
    public ResponseEntity<ShipmentResponse> packShipment(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                         @Valid @RequestBody ShipmentActionRequest request) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(logisticOperatorPort.packShipment(
                principal.user(),
                ShipmentId.of(request.shipmentId()),
                OrderId.of(request.orderId()),
                request.warehouseId() == null ? null : WarehouseId.of(request.warehouseId()),
                request.deliveryAddress() == null ? null : Address.of(request.deliveryAddress()))));
    }

    @PostMapping("/shipments/dispatches")
    public ResponseEntity<ShipmentResponse> dispatchShipment(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                             @Valid @RequestBody ShipmentActionRequest request) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(logisticOperatorPort.dispatchShipment(
                principal.user(), ShipmentId.of(request.shipmentId()), OrderId.of(request.orderId()))));
    }

    @PostMapping("/shipments/deliveries")
    public ResponseEntity<ShipmentResponse> confirmDelivery(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                            @Valid @RequestBody ShipmentActionRequest request) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(logisticOperatorPort.confirmShipmentDelivery(
                principal.user(), ShipmentId.of(request.shipmentId()), OrderId.of(request.orderId()))));
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderResponse>> consultOrdersByStatus(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(
                logisticOperatorPort.consultOrdersByStatus(principal.user(), OrderStatus.valueOf(status)).stream()
                        .map(OrderRestMapper::toResponse)
                        .toList(), page, size));
    }

    private InventoryItem stockOf(DomainUserPrincipal principal, String inventoryItemId) {
        return logisticOperatorPort.consultStockRecord(principal.user(), InventoryItemId.of(inventoryItemId));
    }

    /**
     * The line is a child of the order aggregate, so it is picked from the order the port returns
     * rather than resolved on its own: no repository holds order lines separately.
     */
    private OrderLine lineOf(DomainUserPrincipal principal, StockLineMovementRequest request) {
        Order order = logisticOperatorPort.consultOrder(principal.user(), OrderId.of(request.orderId()));
        return order.getLines().stream()
                .filter(line -> line.getIdentifier().getValue().equals(request.orderLineId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "order line " + request.orderLineId() + " does not belong to order " + request.orderId()));
    }
}
