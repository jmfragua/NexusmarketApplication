package application.domain.services.inventory;

import application.domain.exceptions.DuplicateInventoryItemException;
import application.domain.exceptions.StockRecordNotAllowedException;
import application.domain.models.InventoryItem;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Warehouse;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Opens the stock record of a product in a warehouse: the record every later stock operation acts
 * upon, born empty and available.
 *
 * <p>Stock is distributed, so a record binds exactly one product to one warehouse and the pair is
 * unique (RD-INV-01, RD-ID-05). The seller opens records only for their own products in their own
 * warehouses (RD-CAT-01, RG-03). Opening a record changes no quantity, so it produces no movement:
 * RD-INV-04 governs changes of stock, and the first change is the inbound receipt that follows.</p>
 */
public class OpenStockRecordService {

    private static final String OPERATION = "open stock record";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public OpenStockRecordService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
                                  ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                  ValidateSellerOwnershipService validateSellerOwnershipService) {
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort,
                "the inventory repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @param seller          seller opening the record, owner of both the product and the warehouse.
     * @param product         physical product, not discontinued.
     * @param warehouse       one of the seller's own warehouses.
     * @param inventoryItemId identifier of the new record, supplied by the caller (RD-POUT-04).
     * @return the new record, with zero available and zero reserved units.
     * @throws StockRecordNotAllowedException  when the product is digital (RD-INV-05) or discontinued
     *                                         (RD-CAT-04).
     * @throws DuplicateInventoryItemException when the product already has a record in that warehouse
     *                                         (RD-ID-05) or the identifier is taken.
     */
    public InventoryItem open(Seller seller, Product product, Warehouse warehouse, InventoryItemId inventoryItemId) {
        validateRoleAuthorizationService.validate(seller, OPERATION, UserRole.SELLER);
        Objects.requireNonNull(product, "the product is mandatory");
        Objects.requireNonNull(warehouse, "the warehouse is mandatory");
        Objects.requireNonNull(inventoryItemId, "the stock record identifier is mandatory");
        validateSellerOwnershipService.validateProduct(seller, product);
        validateSellerOwnershipService.validateWarehouse(seller, warehouse);

        if (!product.requiresInventory()) {
            throw StockRecordNotAllowedException.digitalProduct(product.getIdentifier());
        }
        if (product.getStatus().isTerminal()) {
            throw StockRecordNotAllowedException.discontinuedProduct(product.getIdentifier());
        }
        if (inventoryItemRepositoryPort.findByProductAndWarehouse(product.getIdentifier(), warehouse.getIdentifier())
                .isPresent()) {
            throw new DuplicateInventoryItemException(product.getIdentifier(), warehouse.getIdentifier());
        }
        if (inventoryItemRepositoryPort.findById(inventoryItemId).isPresent()) {
            throw new DuplicateInventoryItemException(inventoryItemId);
        }

        InventoryItem inventoryItem = new InventoryItem(inventoryItemId, product.getIdentifier(), warehouse.getIdentifier());
        warehouse.addInventoryItem(inventoryItem);
        return inventoryItemRepositoryPort.save(inventoryItem);
    }
}
