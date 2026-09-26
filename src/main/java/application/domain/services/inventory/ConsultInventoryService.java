package application.domain.services.inventory;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;

/**
 * Consults the distributed stock and its traceability.
 *
 * <p>A buyer never manages inventories (RD-ROL-04): the consultation belongs to the seller, the
 * logistics operator, the administrator and the supervisor (RD-ROL-06).</p>
 */
public class ConsultInventoryService {

    private static final String OPERATION = "consult inventory";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultInventoryService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
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
     * @throws EntityNotFoundException when the product holds no stock record in that warehouse.
     */
    public InventoryItem consultStock(User<?> actor, ProductId productId, WarehouseId warehouseId) {
        validateConsultation(actor);
        return inventoryItemRepositoryPort.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "product " + productId + " holds no stock record in warehouse " + warehouseId));
    }

    public List<InventoryItem> consultByWarehouse(User<?> actor, WarehouseId warehouseId) {
        validateConsultation(actor);
        return inventoryItemRepositoryPort.findByWarehouse(warehouseId);
    }

    public List<InventoryItem> consultByProduct(User<?> actor, ProductId productId) {
        validateConsultation(actor);
        return inventoryItemRepositoryPort.findByProduct(productId);
    }

    /**
     * @return the traceability of a stock record: every movement that affected it (RD-INV-04).
     */
    public List<InventoryMovement> consultMovements(User<?> actor, InventoryItem inventoryItem) {
        validateConsultation(actor);
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        return inventoryMovementRepositoryPort.findByInventoryItem(inventoryItem.getIdentifier());
    }

    private void validateConsultation(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
    }
}
