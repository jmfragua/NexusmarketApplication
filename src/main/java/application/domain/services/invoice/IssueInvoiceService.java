package application.domain.services.invoice;

import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Issues the invoice of an order once its payment has been validated.
 *
 * <p>Invoicing is generated out of the paid order and reflects its commercial value (RD-FAC-01):
 * an order still in {@code CART} or {@code PENDING_PAYMENT} is never invoiced.</p>
 */
public class IssueInvoiceService {

    private static final String OPERATION = "issue invoice";

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public IssueInvoiceService(InvoiceRepositoryPort invoiceRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.invoiceRepositoryPort = Objects.requireNonNull(invoiceRepositoryPort,
                "the invoice repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param invoiceId identifier assigned to the new invoice.
     * @return the issued invoice, whose total matches the one of the order.
     * @throws InvalidOrderTransitionException when the payment of the order is not validated yet.
     */
    public Invoice issue(User<?> actor, Order order, InvoiceId invoiceId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(order, "order is mandatory");
        if (order.getStatus() == OrderStatus.CART || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.PAID);
        }
        Invoice invoice = Invoice.issueFor(invoiceId, order);
        return invoiceRepositoryPort.save(invoice);
    }
}
