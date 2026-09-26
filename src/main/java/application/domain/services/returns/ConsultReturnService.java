package application.domain.services.returns;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.ProductReturn;
import application.domain.models.User;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the after sales returns.
 *
 * <p>A buyer only reaches the returns of their own orders (RD-ROL-04, RD-PED-05); the
 * administrator, who manages the refunds, and the supervisor reach all of them (RD-ROL-06).</p>
 */
public class ConsultReturnService {

    private static final String OPERATION = "consult product return";

    private final ProductReturnRepositoryPort productReturnRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConsultReturnService(ProductReturnRepositoryPort productReturnRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.productReturnRepositoryPort = Objects.requireNonNull(productReturnRepositoryPort,
                "the return repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @throws EntityNotFoundException when no return holds that identifier.
     */
    public ProductReturn consult(User<?> actor, ProductReturnId returnId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR,
                UserRole.LOGISTICS_OPERATOR);
        return productReturnRepositoryPort.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("product return", returnId));
    }

    /**
     * @return the returns registered over one of the own orders of the buyer.
     */
    public List<ProductReturn> consultOwn(Buyer buyer, Order order) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateOrder(buyer, order);
        return productReturnRepositoryPort.findByOrder(order.getIdentifier());
    }
}
