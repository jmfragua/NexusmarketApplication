package application.domain.services.order;

import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Validates the payment of an order and starts the preparation processes (RD-PED-03).
 *
 * <p>Transition {@code PENDING_PAYMENT -> PAID} of the sequential cycle, which admits neither jumps
 * nor steps backwards (RD-PED-01).</p>
 */
public class ConfirmPaymentService {

    private static final String OPERATION = "confirm order payment";

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConfirmPaymentService(OrderRepositoryPort orderRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @throws OrderNotModifiableException      when the order is already finished (RD-PED-02).
     * @throws InvalidOrderTransitionException  when the order is not waiting for payment.
     */
    public Order confirmPayment(Buyer buyer, Order order) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateOrder(buyer, order);
        if (!order.isModifiable()) {
            throw new OrderNotModifiableException(order.getIdentifier());
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.PAID);
        }
        order.confirmPayment();
        return orderRepositoryPort.save(order);
    }
}
