package application.domain.services.order;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the orders of the platform.
 *
 * <p>An order belongs to a single buyer, and only that buyer consults or acts upon it within their
 * role (RD-PED-05, RD-ROL-04). The roles taking part in the management of orders, and the
 * supervisor as a monitoring profile, reach the rest (RD-ROL-06).</p>
 */
public class ConsultOrderService {

    private static final String OPERATION = "consult order";

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConsultOrderService(OrderRepositoryPort orderRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService,
                               ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * Consults one of the own orders of a buyer (RD-PED-05).
     *
     * @throws EntityNotFoundException when no order holds that identifier.
     */
    public Order consultOwn(Buyer buyer, OrderId orderId) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order", orderId));
        validateBuyerOwnershipService.validateOrder(buyer, order);
        return order;
    }

    /**
     * @return every order of the buyer. A buyer never reaches orders of other buyers (RD-ROL-04).
     */
    public List<Order> consultOwn(Buyer buyer) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        return orderRepositoryPort.findByBuyer(buyer.getIdentifier());
    }

    /**
     * @return the orders sitting in a given state, for the roles taking part in their management.
     */
    public List<Order> consultByStatus(User<?> actor, OrderStatus status) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return orderRepositoryPort.findByStatus(status);
    }
}
