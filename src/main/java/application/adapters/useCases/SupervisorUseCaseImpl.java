package application.adapters.useCases;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.User;
import application.domain.ports.in.SupervisorPort;
import application.domain.services.order.ConsultOrderService;
import application.domain.services.refund.ConsultRefundService;
import application.domain.services.reporting.ConsultInventoryReportService;
import application.domain.services.reporting.ConsultSalesReportService;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Implements the supervisor entry port by delegating to the domain services.
 *
 * <p>Every operation is read only: the supervisor never executes modification processes
 * (RD-ROL-06), which is why this use case injects no service that writes.</p>
 */
@Service
public class SupervisorUseCaseImpl implements SupervisorPort {

    private final ConsultOrderService consultOrderService;

    private final ConsultSalesReportService consultSalesReportService;

    private final ConsultInventoryReportService consultInventoryReportService;

    private final ConsultRefundService consultRefundService;

    public SupervisorUseCaseImpl(ConsultOrderService consultOrderService,
                                 ConsultSalesReportService consultSalesReportService,
                                 ConsultInventoryReportService consultInventoryReportService,
                                 ConsultRefundService consultRefundService) {
        this.consultOrderService = Objects.requireNonNull(consultOrderService);
        this.consultSalesReportService = Objects.requireNonNull(consultSalesReportService);
        this.consultInventoryReportService = Objects.requireNonNull(consultInventoryReportService);
        this.consultRefundService = Objects.requireNonNull(consultRefundService);
    }

    @Override
    public List<Order> consultOrdersByStatus(User<?> supervisor, OrderStatus status) {
        return consultOrderService.consultByStatus(supervisor, status);
    }

    @Override
    public List<Order> consultCompletedOrders(User<?> supervisor) {
        return consultSalesReportService.consultCompletedOrders(supervisor);
    }

    @Override
    public List<Invoice> consultIssuedInvoices(User<?> supervisor) {
        return consultSalesReportService.consultIssuedInvoices(supervisor);
    }

    @Override
    public Optional<Money> consultInvoicedTotal(User<?> supervisor) {
        return consultSalesReportService.consultInvoicedTotal(supervisor);
    }

    @Override
    public List<InventoryItem> consultStockByWarehouse(User<?> supervisor, WarehouseId warehouseId) {
        return consultInventoryReportService.consultStockByWarehouse(supervisor, warehouseId);
    }

    @Override
    public List<InventoryItem> consultDamagedStock(User<?> supervisor) {
        return consultInventoryReportService.consultDamagedStock(supervisor);
    }

    @Override
    public List<InventoryMovement> consultInventoryMovements(User<?> supervisor) {
        return consultInventoryReportService.consultMovements(supervisor);
    }

    @Override
    public List<Refund> consultRefunds(User<?> supervisor) {
        return consultRefundService.consultAll(supervisor);
    }
}
