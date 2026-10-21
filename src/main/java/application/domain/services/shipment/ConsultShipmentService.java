package application.domain.services.shipment;

import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the shipments of an order. Read only.
 *
 * <p>The operation roles see the shipments of any order; a buyer sees only those of an order of
 * their own (RD-PED-05, RD-ROL-04). A shipment carries no status of its own — the logistic progress
 * lives on the order (RD-ALC-03) — so what this answers is where the order ships from and to, and
 * whether it has been packed at all.</p>
 */
public class ConsultShipmentService {

    private static final String OPERATION = "consult shipments";

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConsultShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                  ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.shipmentRepositoryPort = Objects.requireNonNull(shipmentRepositoryPort,
                "the shipment repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @return the shipments of the order; empty while it has not been packed.
     */
    public List<Shipment> consultByOrder(User<?> actor, Order order) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        Objects.requireNonNull(order, "the order is mandatory");
        return shipmentRepositoryPort.findByOrder(order.getIdentifier());
    }

    /**
     * @return the shipments of an order of the buyer; empty while it has not been packed.
     */
    public List<Shipment> consultOwn(Buyer buyer, Order order) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateOrder(buyer, order);
        return shipmentRepositoryPort.findByOrder(order.getIdentifier());
    }
}
