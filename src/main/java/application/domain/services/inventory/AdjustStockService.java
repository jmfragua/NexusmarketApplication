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
 * Corrects the registered available stock, which produces an adjustment movement (RD-INV-04).
 *
 * <p>The balance never becomes negative, because {@link Quantity} makes a negative value impossible
 * to build (RD-INV-02).</p>
 */
public class AdjustStockService {

    private static final String OPERATION = "adjust stock";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public AdjustStockService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
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
     * @param newAvailableQuantity corrected available stock.
     * @return the adjustment movement recorded in the traceability of the stock.
     */
    public InventoryMovement adjust(User<?> actor,
                                    InventoryItem inventoryItem,
                                    Quantity newAvailableQuantity,
                                    InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        InventoryMovement movement = inventoryItem.adjust(newAvailableQuantity, movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
