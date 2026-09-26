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
 * Confirms the delivery of the shipment, which closes the order at {@code DELIVERED}.
 *
 * <p>The order is marked as finished after the confirmed delivery and from that moment it can never
 * be modified under any circumstance (RD-PED-02).</p>
 */
public class ConfirmShipmentDeliveryService {

    private static final String OPERATION = "confirm shipment delivery";

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConfirmShipmentDeliveryService(ShipmentRepositoryPort shipmentRepositoryPort,
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
     * @throws InvalidOrderTransitionException when the order has not been dispatched yet.
     */
    public Shipment confirmDelivery(User<?> actor, Shipment shipment, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(shipment, "shipment is mandatory");
        Objects.requireNonNull(order, "order is mandatory");
        if (!order.isModifiable()) {
            throw new OrderNotModifiableException(order.getIdentifier());
        }
        if (!order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
            throw new InvalidOrderTransitionException(order.getIdentifier(), order.getStatus(), OrderStatus.DELIVERED);
        }
        shipment.confirmDelivery(order);
        orderRepositoryPort.save(order);
        return shipmentRepositoryPort.save(shipment);
    }
}
