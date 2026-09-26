package application.domain.services.shipment;

import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Registers the physical exit of the shipment from the warehouse, which moves the order to
 * {@code DISPATCHED}.
 *
 * <p>Every shipment departs from a concrete warehouse, the one supporting the stock exit
 * (RD-LOG-02). The specification declares no state catalogue of its own for the shipment: the
 * logistic progress is reflected on the state of the order (RD-ALC-03).</p>
 */
public class DispatchShipmentService {

    private static final String OPERATION = "dispatch shipment";

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public DispatchShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                                   OrderRepositoryPort orderRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = Objects.requireNonNull(shipmentRepositoryPort,
                "the shipment repository is mandatory");
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @throws OrderNotModifiableException     when the order is already finished (RD-PED-02).
     * @throws InvalidOrderTransitionException when the order has not been paid yet (RD-PED-01).
     */
    public Shipment dispatch(User<?> actor, Shipment shipment, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(shipment, "shipment is mandatory");
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.isModifiable()) {
            throw new OrderNotModifiableException(order.getIdentifier());
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.DISPATCHED)) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.DISPATCHED);
        }
        shipment.dispatch(order);
        orderRepositoryPort.save(order);
        return shipmentRepositoryPort.save(shipment);
    }
}
