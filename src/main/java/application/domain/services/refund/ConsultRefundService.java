package application.domain.services.refund;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.User;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the refunds of the platform.
 *
 * <p>The management of refunds is a shared responsibility between the buyer, who requests them, and
 * the administrator, who manages them (RD-ROL-06).</p>
 */
public class ConsultRefundService {

    private static final String OPERATION = "consult refund";

    private final RefundRepositoryPort refundRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultRefundService(RefundRepositoryPort refundRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.refundRepositoryPort = Objects.requireNonNull(refundRepositoryPort, "the refund repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @throws EntityNotFoundException when no refund holds that identifier.
     */
    public Refund consult(User<?> actor, RefundId refundId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return refundRepositoryPort.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("refund", refundId));
    }

    /**
     * @return the refund derived from a return, empty while it has not been issued (RD-POS-02).
     */
    public Refund consultByReturn(User<?> actor, ProductReturn productReturn) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        Objects.requireNonNull(productReturn, "the return is mandatory");
        return refundRepositoryPort.findByProductReturn(productReturn.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException(
                        "the return " + productReturn.getIdentifier() + " has not been refunded yet"));
    }

    public List<Refund> consultAll(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return refundRepositoryPort.findAll();
    }
}
