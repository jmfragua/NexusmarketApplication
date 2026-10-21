package application.domain.services.shipment;

import application.domain.exceptions.DuplicateIdentifierException;
import application.domain.exceptions.ShipmentNotAllowedException;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Collection;
import java.util.Objects;

/**
 * Prepares the packing of an order, the first step of the logistic process.
 *
 * <p>Only orders holding physical products generate shipments: digital products are delivered
 * immediately once the payment is confirmed (RD-LOG-01). Packing requires a validated payment
 * (RD-PED-03).</p>
 */
public class PackShipmentService {

    private static final String OPERATION = "pack shipment";

    private final ShipmentRepositoryPort shipmentRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public PackShipmentService(ShipmentRepositoryPort shipmentRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.shipmentRepositoryPort = Objects.requireNonNull(shipmentRepositoryPort,
                "the shipment repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param products the products referenced by the lines of the order, which decide whether
     *                 logistics are needed at all.
     * @return the packed shipment.
     * @throws ShipmentNotAllowedException when the order holds no physical products.
     */
    public Shipment pack(User<?> actor, Shipment shipment, Order order, Collection<Product> products) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(shipment, "shipment is mandatory");
        Objects.requireNonNull(order, "order is mandatory");
        if (!shipment.isFor(order)) {
            // The identifier names the shipment of another order: packing must not take it over.
            throw new DuplicateIdentifierException("shipment", shipment.getIdentifier());
        }
        if (!order.containsPhysicalProducts(products)) {
            throw new ShipmentNotAllowedException(order.getIdentifier());
        }
        boolean alreadyOpened = shipmentRepositoryPort.findById(shipment.getIdentifier()).isPresent();
        shipment.pack(order);
        return alreadyOpened ? shipmentRepositoryPort.save(shipment) : shipmentRepositoryPort.create(shipment);
    }
}
