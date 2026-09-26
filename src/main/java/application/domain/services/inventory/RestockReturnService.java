package application.domain.services.inventory;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.ProductReturn;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Reintegrates into the warehouse the stock coming from a return, which produces a return movement
 * (RD-POS-01, RD-INV-04).
 */
public class RestockReturnService {

    private static final String OPERATION = "restock returned units";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RestockReturnService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
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
     * @param productReturn return whose units come back to the warehouse.
     * @return the return movement recorded in the traceability of the stock.
     */
    public InventoryMovement restock(User<?> actor,
                                     ProductReturn productReturn,
                                     InventoryItem inventoryItem,
                                     InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR, UserRole.SELLER);
        Objects.requireNonNull(productReturn, "the return is mandatory");
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        InventoryMovement movement = productReturn.restock(inventoryItem, movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
