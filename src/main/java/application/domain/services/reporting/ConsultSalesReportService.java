package application.domain.services.reporting;

import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Consolidates the commercial information of the platform for consultation.
 *
 * <p>Administrative reporting owns no identity nor lifecycle of its own and never modifies the
 * state of the business: it is resolved as read only queries over the existing entities, available
 * to the administrator and the supervisor (RD-ROL-06).</p>
 */
public class ConsultSalesReportService {

    private static final String OPERATION = "consult sales report";

    private final OrderRepositoryPort orderRepositoryPort;

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultSalesReportService(OrderRepositoryPort orderRepositoryPort,
                                     InvoiceRepositoryPort invoiceRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.invoiceRepositoryPort = Objects.requireNonNull(invoiceRepositoryPort,
                "the invoice repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @return the orders that reached the end of their cycle.
     */
    public List<Order> consultCompletedOrders(User<?> actor) {
        validateReporting(actor);
        return orderRepositoryPort.findByStatus(OrderStatus.DELIVERED);
    }

    /**
     * @return every invoice issued by the platform.
     */
    public List<Invoice> consultIssuedInvoices(User<?> actor) {
        validateReporting(actor);
        return invoiceRepositoryPort.findAll();
    }

    /**
     * Adds up the invoiced value of the platform.
     *
     * <p>Only amounts of the same currency can be added, so the total is empty while no invoice has
     * been issued.</p>
     *
     * @return the invoiced total, empty when there is nothing to add up.
     */
    public Optional<Money> consultInvoicedTotal(User<?> actor) {
        validateReporting(actor);
        return invoiceRepositoryPort.findAll().stream()
                .map(Invoice::getTotalAmount)
                .reduce(Money::add);
    }

    private void validateReporting(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
    }
}
