package application.domain.services.warehouse;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;

/**
 * Consults the warehouses where the distributed inventory is held (RD-INV-01).
 *
 * <p>A seller only reaches their own warehouses (RG-03); the logistics operator, who runs the
 * physical operation, the administrator and the supervisor reach all of them (RD-ROL-06).</p>
 */
public class ConsultWarehouseService {

    private static final String OPERATION = "consult warehouse";

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public ConsultWarehouseService(WarehouseRepositoryPort warehouseRepositoryPort,
                                   ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                   ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.warehouseRepositoryPort = Objects.requireNonNull(warehouseRepositoryPort,
                "the warehouse repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @throws EntityNotFoundException when no warehouse holds that identifier.
     */
    public Warehouse consult(User<?> actor, WarehouseId warehouseId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return requireWarehouse(warehouseId);
    }

    /**
     * Consults one of the own warehouses of a seller (RG-03).
     */
    public Warehouse consultOwn(Seller seller, WarehouseId warehouseId) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        Warehouse warehouse = requireWarehouse(warehouseId);
        validateSellerOwnershipService.validateWarehouse(seller, warehouse);
        return warehouse;
    }

    /**
     * @return every warehouse owned by the seller.
     */
    public List<SellerWarehouse> consultOwn(Seller seller) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        return warehouseRepositoryPort.findBySeller(seller.getIdentifier());
    }

    public List<Warehouse> consultAll(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.LOGISTICS_OPERATOR,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return warehouseRepositoryPort.findAll();
    }

    private Warehouse requireWarehouse(WarehouseId warehouseId) {
        return warehouseRepositoryPort.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("warehouse", warehouseId));
    }
}
