package application.domain.services.inventory;

import application.domain.models.InventoryItem;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Marks the stock of a warehouse as damaged, which excludes it from every reservation (RD-INV-03).
 *
 * <p>The condition of the stock is a one way transition: {@code AVAILABLE -> DAMAGED}
 * (RD-VO-12).</p>
 */
public class MarkDamagedService {

    private static final String OPERATION = "mark stock as damaged";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public MarkDamagedService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort,
                "the inventory repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    public InventoryItem markAsDamaged(User<?> actor, InventoryItem inventoryItem) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        inventoryItem.markAsDamaged();
        return inventoryItemRepositoryPort.save(inventoryItem);
    }
}
