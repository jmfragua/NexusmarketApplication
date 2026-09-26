package application.domain.services.refund;

import application.domain.exceptions.RefundExceedsInvoiceException;
import application.domain.models.Invoice;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.User;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Issues the refund of a return, applied on the invoice of the order it originates from.
 *
 * <p>Every refund derives from a return (RD-POS-02) and its management belongs to the administrator
 * (RD-ROL-06). The refunded amount never exceeds the invoiced total.</p>
 */
public class IssueRefundService {

    private static final String OPERATION = "issue refund";

    private final RefundRepositoryPort refundRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public IssueRefundService(RefundRepositoryPort refundRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.refundRepositoryPort = Objects.requireNonNull(refundRepositoryPort, "the refund repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param productReturn return giving rise to the refund.
     * @param invoice       invoice of the order the refund is applied on.
     * @param amount        refunded value.
     * @return the issued refund.
     * @throws RefundExceedsInvoiceException when the amount exceeds the invoiced total.
     */
    public Refund issue(User<?> actor, ProductReturn productReturn, Invoice invoice, Money amount, RefundId refundId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(productReturn, "the return is mandatory");
        Objects.requireNonNull(invoice, "invoice is mandatory");
        Objects.requireNonNull(amount, "the refunded amount is mandatory");
        if (amount.isGreaterThan(invoice.getTotalAmount())) {
            throw new RefundExceedsInvoiceException(invoice.getIdentifier(), amount);
        }
        Refund refund = Refund.issueFor(refundId, productReturn, invoice, amount);
        return refundRepositoryPort.save(refund);
    }
}
