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
 * Registers the physical exit of the order from the warehouse.
 *
 * <p>Transition {@code PAID -> DISPATCHED} of the sequential cycle (RD-PED-01). The physical
 * operation of warehouses and dispatches belongs to the logistics operator (RD-ROL-06).</p>
 */
public class DispatchOrderService {

    private static final String OPERATION = "dispatch order";

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public DispatchOrderService(OrderRepositoryPort orderRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @throws OrderNotModifiableException     when the order is already finished (RD-PED-02).
     * @throws InvalidOrderTransitionException when the order has not been paid yet.
     */
    public Order dispatch(User<?> actor, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.isModifiable()) {
            throw new OrderNotModifiableException(order.getIdentifier());
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.DISPATCHED)) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.DISPATCHED);
        }
        order.dispatch();
        return orderRepositoryPort.save(order);
    }
}
