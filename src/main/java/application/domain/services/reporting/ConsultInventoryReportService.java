package application.domain.services.reporting;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;

/**
 * Consolidates the information of the distributed inventory for consultation.
 *
 * <p>Read only queries over the existing stock records and their traceability, available to the
 * administrator and the supervisor (RD-ROL-06). The inventory is distributed: every figure is bound
 * to a specific warehouse and there is no global stock (RD-INV-01).</p>
 */
public class ConsultInventoryReportService {

    private static final String OPERATION = "consult inventory report";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultInventoryReportService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
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
     * @return the stock held in a specific warehouse (RD-INV-01).
     */
    public List<InventoryItem> consultStockByWarehouse(User<?> actor, WarehouseId warehouseId) {
        validateReporting(actor);
        return inventoryItemRepositoryPort.findByWarehouse(warehouseId);
    }

    /**
     * @return the stock records excluded from every reservation because they are damaged
     *         (RD-INV-03).
     */
    public List<InventoryItem> consultDamagedStock(User<?> actor) {
        validateReporting(actor);
        return inventoryItemRepositoryPort.findAll().stream()
                .filter(item -> item.getCondition() == StockCondition.DAMAGED)
                .toList();
    }

    /**
     * @return the whole traceability of the inventory: every movement of the five defined types
     *         (RD-INV-04).
     */
    public List<InventoryMovement> consultMovements(User<?> actor) {
        validateReporting(actor);
        return inventoryMovementRepositoryPort.findAll();
    }

    private void validateReporting(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
    }
}
