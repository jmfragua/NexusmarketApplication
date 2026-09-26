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
 * Frees the stock committed by an order line and returns it to the available balance.
 *
 * <p>It is recorded as an adjustment, the movement type the business defines to correct the
 * registered stock (RD-INV-04).</p>
 */
public class ReleaseReservationService {

    private static final String OPERATION = "release stock reservation";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ReleaseReservationService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
                                     InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                                     ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort,
                "the inventory repository is mandatory");
        this.inventoryMovementRepositoryPort = Objects.requireNonNull(inventoryMovementRepositoryPort,
                "the movement repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    public InventoryMovement release(User<?> actor,
                                     OrderLine orderLine,
                                     InventoryItem inventoryItem,
                                     InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(orderLine, "order line is mandatory");
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        InventoryMovement movement = orderLine.releaseStock(inventoryItem, movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
