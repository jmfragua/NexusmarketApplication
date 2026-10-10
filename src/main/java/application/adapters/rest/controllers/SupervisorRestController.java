package application.adapters.rest.controllers;

import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
import application.adapters.rest.dtos.responses.InvoiceResponse;
import application.adapters.rest.dtos.responses.OrderResponse;
import application.adapters.rest.dtos.responses.RefundResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.domain.ports.in.SupervisorPort;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.WarehouseId;
import application.infrastructure.security.DomainUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supervisor endpoints: consolidated administrative reporting (OBJ-12).
 *
 * <p>Every endpoint is a {@code GET}. The supervisor is a consultation and monitoring profile and
 * never executes modification processes (RD-ROL-06) — the absence of any write verb here is that
 * rule made visible in the API surface.</p>
 */
@RestController
@RequestMapping("/api/v1/supervisor")
public class SupervisorRestController {

    private final SupervisorPort supervisorPort;

    public SupervisorRestController(SupervisorPort supervisorPort) {
        this.supervisorPort = Objects.requireNonNull(supervisorPort);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> consultOrdersByStatus(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam String status) {
        return ResponseEntity.ok(
                supervisorPort.consultOrdersByStatus(principal.user(), OrderStatus.valueOf(status)).stream()
                        .map(OrderRestMapper::toResponse)
                        .toList());
    }

    @GetMapping("/reports/completed-orders")
    public ResponseEntity<List<OrderResponse>> consultCompletedOrders(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(supervisorPort.consultCompletedOrders(principal.user()).stream()
                .map(OrderRestMapper::toResponse)
                .toList());
    }

    @GetMapping("/reports/invoices")
    public ResponseEntity<List<InvoiceResponse>> consultIssuedInvoices(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(supervisorPort.consultIssuedInvoices(principal.user()).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList());
    }

    /**
     * Returns the invoiced total, or {@code 204 No Content} when nothing has been invoiced yet.
     *
     * <p>The empty case is real rather than a zero: amounts of different currencies cannot be added
     * and there is no neutral {@code Money} without a currency, so the domain refuses to invent one
     * (RD-ALC-03).</p>
     */
    @GetMapping("/reports/invoiced-total")
    public ResponseEntity<Map<String, BigDecimal>> consultInvoicedTotal(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return supervisorPort.consultInvoicedTotal(principal.user())
                .map(total -> ResponseEntity.ok(Map.of(total.getCurrency(), total.getAmount())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/reports/warehouses/{warehouseId}/stock")
    public ResponseEntity<List<InventoryItemResponse>> consultStockByWarehouse(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String warehouseId) {
        return ResponseEntity.ok(
                supervisorPort.consultStockByWarehouse(principal.user(), WarehouseId.of(warehouseId)).stream()
                        .map(InventoryRestMapper::toResponse)
                        .toList());
    }

    @GetMapping("/reports/damaged-stock")
    public ResponseEntity<List<InventoryItemResponse>> consultDamagedStock(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(supervisorPort.consultDamagedStock(principal.user()).stream()
                .map(InventoryRestMapper::toResponse)
                .toList());
    }

    @GetMapping("/reports/inventory-movements")
    public ResponseEntity<List<InventoryMovementResponse>> consultInventoryMovements(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(supervisorPort.consultInventoryMovements(principal.user()).stream()
                .map(InventoryRestMapper::toResponse)
                .toList());
    }

    @GetMapping("/reports/refunds")
    public ResponseEntity<List<RefundResponse>> consultRefunds(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(supervisorPort.consultRefunds(principal.user()).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList());
    }
}
