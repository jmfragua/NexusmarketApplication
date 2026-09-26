package application.domain.services.order;

import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Closes the order once the delivery is confirmed.
 *
 * <p>Transition {@code DISPATCHED -> DELIVERED} of the sequential cycle (RD-PED-01).
 * {@code DELIVERED} is terminal: from that moment the order can never be modified under any
 * circumstance (RD-PED-02, RD-VO-13).</p>
 */
public class CompleteDeliveryService {

    private static final String OPERATION = "complete order delivery";

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public CompleteDeliveryService(OrderRepositoryPort orderRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @throws OrderNotModifiableException     when the order is already finished (RD-PED-02).
     * @throws InvalidOrderTransitionException when the order has not been dispatched yet.
     */
    public Order completeDelivery(User<?> actor, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.isModifiable()) {
            throw new OrderNotModifiableException(order.getIdentifier());
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.DELIVERED);
        }
        order.completeDelivery();
        return orderRepositoryPort.save(order);
    }
}
