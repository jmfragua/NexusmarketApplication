package application.domain.services.inventory;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Registers stock into a warehouse, which produces an inbound movement.
 *
 * <p>The administration of inventory is shared by the seller and the logistics operator
 * (RD-ROL-06). No stock change exists without an associated movement (RD-INV-04) and stock is
 * always bound to one product and one specific warehouse (RD-INV-01).</p>
 */
public class ReceiveStockService {

    private static final String OPERATION = "receive stock";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ReceiveStockService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
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
     * @param actor         seller or logistics operator registering the stock.
     * @param inventoryItem stock record of the product in the warehouse.
     * @param quantity      units entering the warehouse, never zero.
     * @param movementId    identifier assigned to the resulting movement.
     * @return the inbound movement recorded in the traceability of the stock.
     */
    public InventoryMovement receive(User<?> actor,
                                     InventoryItem inventoryItem,
                                     Quantity quantity,
                                     InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        InventoryMovement movement = inventoryItem.receive(quantity, movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
