package application.domain.services.warehouse;

import application.domain.models.MarketplaceWarehouse;
import application.domain.models.User;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.WarehouseId;
import java.util.Objects;

/**
 * Registers a warehouse owned by the marketplace and operated directly by the platform.
 *
 * <p>The administration of warehouses belongs to the administrator (RD-ROL-06); the warehouses of a
 * seller are instead registered along with the incorporation of the seller (RD-ROL-05).</p>
 */
public class RegisterMarketplaceWarehouseService {

    private static final String OPERATION = "register marketplace warehouse";

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RegisterMarketplaceWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort,
                                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.warehouseRepositoryPort = Objects.requireNonNull(warehouseRepositoryPort,
                "the warehouse repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param actor       administrator registering the warehouse.
     * @param warehouseId identifier assigned to the new warehouse.
     * @return the registered warehouse.
     */
    public MarketplaceWarehouse register(User<?> actor, WarehouseId warehouseId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        MarketplaceWarehouse warehouse = new MarketplaceWarehouse(warehouseId);
        warehouseRepositoryPort.save(warehouse);
        return warehouse;
    }
}
