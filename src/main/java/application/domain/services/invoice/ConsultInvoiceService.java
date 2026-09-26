package application.domain.services.invoice;

import application.domain.exceptions.InvoiceNotIssuedException;
import application.domain.models.Buyer;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the invoicing of the platform.
 *
 * <p>A buyer only reaches their own invoices (RD-ROL-04); the administrator and the supervisor
 * consult the invoicing as consolidated administrative information (RD-ROL-06).</p>
 */
public class ConsultInvoiceService {

    private static final String OPERATION = "consult invoice";

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConsultInvoiceService(InvoiceRepositoryPort invoiceRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.invoiceRepositoryPort = Objects.requireNonNull(invoiceRepositoryPort,
                "the invoice repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @throws InvoiceNotIssuedException when the order has not been invoiced yet.
     */
    public Invoice consultByOrder(User<?> actor, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.ADMINISTRATOR,
                UserRole.SUPERVISOR);
        Objects.requireNonNull(order, "order is mandatory");
        return requireInvoiceOf(order.getIdentifier());
    }

    /**
     * Consults the invoice of one of the own orders of a buyer.
     */
    public Invoice consultOwn(Buyer buyer, Order order) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateOrder(buyer, order);
        Invoice invoice = requireInvoiceOf(order.getIdentifier());
        validateBuyerOwnershipService.validateInvoice(buyer, invoice);
        return invoice;
    }

    /**
     * @return every invoice issued to the buyer.
     */
    public List<Invoice> consultOwn(Buyer buyer) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        return invoiceRepositoryPort.findByBuyer(buyer.getIdentifier());
    }

    private Invoice requireInvoiceOf(OrderId orderId) {
        return invoiceRepositoryPort.findByOrder(orderId)
                .orElseThrow(() -> new InvoiceNotIssuedException(orderId));
    }
}
