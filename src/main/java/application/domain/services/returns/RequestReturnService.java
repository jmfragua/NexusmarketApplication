package application.domain.services.returns;

import application.domain.exceptions.ReturnNotAllowedException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.ProductReturn;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Registers the return of a line of a delivered order.
 *
 * <p>Every return originates in a line of an existing order (RD-POS-01) and the after sales cycle
 * only starts once the order is finished. The returned quantity never exceeds the one of the
 * line.</p>
 */
public class RequestReturnService {

    private static final String OPERATION = "request product return";

    private final ProductReturnRepositoryPort productReturnRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public RequestReturnService(ProductReturnRepositoryPort productReturnRepositoryPort,
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
     * @param returnId  identifier assigned to the new return.
     * @param orderLine line of the order being returned.
     * @param quantity  returned units, never exceeding the ones of the line.
     * @return the registered return.
     * @throws ReturnNotAllowedException when the order has not been delivered yet.
     */
    public ProductReturn requestReturn(Buyer buyer,
                                       Order order,
                                       OrderLine orderLine,
                                       Quantity quantity,
                                       ProductReturnId returnId) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateOrder(buyer, order);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ReturnNotAllowedException(order.getIdentifier());
        }
        ProductReturn productReturn = buyer.requestReturn(returnId, order, orderLine, quantity);
        return productReturnRepositoryPort.save(productReturn);
    }
}
