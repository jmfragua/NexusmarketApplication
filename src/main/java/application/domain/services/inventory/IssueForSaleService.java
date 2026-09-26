package application.domain.services.inventory;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.OrderLine;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Takes the committed stock physically out of the warehouse because the order was dispatched, which
 * produces a sale outbound movement (RD-INV-04).
 *
 * <p>Every shipment departs from a concrete warehouse, the one supporting the stock exit
 * (RD-LOG-02), and the physical operation of warehouses belongs to the logistics operator
 * (RD-ROL-06).</p>
 */
public class IssueForSaleService {

    private static final String OPERATION = "issue stock for sale";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public IssueForSaleService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
                               InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort,
                "the inventory repository is mandatory");
        this.inventoryMovementRepositoryPort = Objects.requireNonNull(inventoryMovementRepositoryPort,
                "the movement repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param orderLine line of the dispatched order.
     * @return the sale outbound movement recorded in the traceability of the stock.
     */
    public InventoryMovement issue(User<?> actor,
                                   OrderLine orderLine,
                                   InventoryItem inventoryItem,
                                   InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(orderLine, "order line is mandatory");
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        InventoryMovement movement = inventoryItem.issueForSale(orderLine.getQuantity(), movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
