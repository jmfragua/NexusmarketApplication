package application.adapters.useCases;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.ports.in.SellerPort;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.catalog.AddProductVariantService;
import application.domain.services.catalog.ConsultCatalogService;
import application.domain.services.catalog.DiscontinueProductService;
import application.domain.services.catalog.PublishProductService;
import application.domain.services.catalog.RegisterProductService;
import application.domain.services.catalog.RemoveProductVariantService;
import application.domain.services.catalog.SuspendProductService;
import application.domain.services.inventory.AdjustStockService;
import application.domain.services.inventory.ConsultInventoryService;
import application.domain.services.inventory.MarkDamagedService;
import application.domain.services.inventory.OpenStockRecordService;
import application.domain.services.inventory.ReceiveStockService;
import application.domain.services.warehouse.ConsultWarehouseService;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implements the seller entry port by delegating to the domain services.
 */
@Service
public class SellerUseCaseImpl implements SellerPort {

    private final RegisterProductService registerProductService;

    private final PublishProductService publishProductService;

    private final SuspendProductService suspendProductService;

    private final DiscontinueProductService discontinueProductService;

    private final AddProductVariantService addProductVariantService;

    private final RemoveProductVariantService removeProductVariantService;

    private final ConsultCatalogService consultCatalogService;

    private final ConsultWarehouseService consultWarehouseService;

    private final OpenStockRecordService openStockRecordService;

    private final ReceiveStockService receiveStockService;

    private final AdjustStockService adjustStockService;

    private final MarkDamagedService markDamagedService;

    private final ConsultInventoryService consultInventoryService;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    private final ProductRepositoryPort productRepositoryPort;

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    public SellerUseCaseImpl(RegisterProductService registerProductService,
                             PublishProductService publishProductService,
                             SuspendProductService suspendProductService,
                             DiscontinueProductService discontinueProductService,
                             AddProductVariantService addProductVariantService,
                             RemoveProductVariantService removeProductVariantService,
                             ConsultCatalogService consultCatalogService,
                             ConsultWarehouseService consultWarehouseService,
                             OpenStockRecordService openStockRecordService,
                             ReceiveStockService receiveStockService,
                             AdjustStockService adjustStockService,
                             MarkDamagedService markDamagedService,
                             ConsultInventoryService consultInventoryService,
                             ValidateRoleAuthorizationService validateRoleAuthorizationService,
                             ValidateSellerOwnershipService validateSellerOwnershipService,
                             ProductRepositoryPort productRepositoryPort,
                             InventoryItemRepositoryPort inventoryItemRepositoryPort,
                             WarehouseRepositoryPort warehouseRepositoryPort) {
        this.registerProductService = Objects.requireNonNull(registerProductService);
        this.publishProductService = Objects.requireNonNull(publishProductService);
        this.suspendProductService = Objects.requireNonNull(suspendProductService);
        this.discontinueProductService = Objects.requireNonNull(discontinueProductService);
        this.addProductVariantService = Objects.requireNonNull(addProductVariantService);
        this.removeProductVariantService = Objects.requireNonNull(removeProductVariantService);
        this.consultCatalogService = Objects.requireNonNull(consultCatalogService);
        this.consultWarehouseService = Objects.requireNonNull(consultWarehouseService);
        this.openStockRecordService = Objects.requireNonNull(openStockRecordService);
        this.receiveStockService = Objects.requireNonNull(receiveStockService);
        this.adjustStockService = Objects.requireNonNull(adjustStockService);
        this.markDamagedService = Objects.requireNonNull(markDamagedService);
        this.consultInventoryService = Objects.requireNonNull(consultInventoryService);
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService);
        this.validateSellerOwnershipService = Objects.requireNonNull(validateSellerOwnershipService);
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort);
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort);
        this.warehouseRepositoryPort = Objects.requireNonNull(warehouseRepositoryPort);
    }

    /**
     * Resolves the product and validates ownership while loading it, so a seller asking for someone
     * else's product is rejected at this point rather than later (RD-CAT-01).
     *
     * <p>The role and the active status are checked first, before the lookup: a blocked seller must
     * not be able to tell from the status code whether a product exists (RG-01, RD-ROL-03).</p>
     */
    @Override
    public Product consultMyProduct(Seller seller, ProductId productId) {
        validateRoleAuthorizationService.validate(seller, "consult own product", UserRole.SELLER);
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("product", productId));
        validateSellerOwnershipService.validateProduct(seller, product);
        return product;
    }

    /**
     * Resolves a stock record and validates, while loading it, that both its product and its
     * warehouse are the seller's: a seller never operates on the stock of another (RD-CAT-01, RG-03).
     */
    @Override
    public InventoryItem consultStockRecord(Seller seller, InventoryItemId inventoryItemId) {
        validateRoleAuthorizationService.validate(seller, "consult stock record", UserRole.SELLER);
        InventoryItem inventoryItem = inventoryItemRepositoryPort.findById(inventoryItemId)
                .orElseThrow(() -> new EntityNotFoundException("stock record", inventoryItemId));
        return requireOwnStockRecord(seller, inventoryItem);
    }

    @Override
    public Product registerProduct(Seller seller, Product product) {
        return registerProductService.register(seller, product);
    }

    @Override
    public Product publishProduct(Seller seller, Product product) {
        return publishProductService.publish(seller, product);
    }

    @Override
    public Product suspendProduct(Seller seller, Product product) {
        return suspendProductService.suspend(seller, product);
    }

    @Override
    public Product discontinueProduct(Seller seller, Product product) {
        return discontinueProductService.discontinue(seller, product);
    }

    @Override
    public Product addProductVariant(Seller seller, Product product, ProductVariant variant) {
        return addProductVariantService.addVariant(seller, product, variant);
    }

    @Override
    public Product removeProductVariant(Seller seller, Product product, ProductVariant variant) {
        return removeProductVariantService.removeVariant(seller, product, variant);
    }

    @Override
    public List<Product> consultMyCatalog(Seller seller) {
        return consultCatalogService.consultOwnCatalog(seller);
    }

    @Override
    public List<SellerWarehouse> consultMyWarehouses(Seller seller) {
        return consultWarehouseService.consultOwn(seller);
    }

    @Override
    public Warehouse consultMyWarehouse(Seller seller, WarehouseId warehouseId) {
        return consultWarehouseService.consultOwn(seller, warehouseId);
    }

    @Override
    public InventoryItem openStockRecord(Seller seller, Product product, Warehouse warehouse,
                                         InventoryItemId inventoryItemId) {
        return openStockRecordService.open(seller, product, warehouse, inventoryItemId);
    }

    @Override
    public InventoryMovement receiveStock(Seller seller, InventoryItem inventoryItem, Quantity quantity,
                                          InventoryMovementId movementId) {
        requireOwnStockRecord(seller, inventoryItem);
        return receiveStockService.receive(seller, inventoryItem, quantity, movementId);
    }

    @Override
    public InventoryMovement adjustStock(Seller seller, InventoryItem inventoryItem, Quantity newAvailableQuantity,
                                         InventoryMovementId movementId) {
        requireOwnStockRecord(seller, inventoryItem);
        return adjustStockService.adjust(seller, inventoryItem, newAvailableQuantity, movementId);
    }

    @Override
    public InventoryItem markStockAsDamaged(Seller seller, InventoryItem inventoryItem) {
        requireOwnStockRecord(seller, inventoryItem);
        return markDamagedService.markAsDamaged(seller, inventoryItem);
    }

    @Override
    public List<InventoryItem> consultStockOfMyProduct(Seller seller, ProductId productId) {
        Product product = consultMyProduct(seller, productId);
        return consultInventoryService.consultByProduct(seller, product.getIdentifier());
    }

    /**
     * The validation lives in the domain ({@link ValidateSellerOwnershipService#validateStockRecord});
     * this only resolves the product and the warehouse the record points to.
     */
    private InventoryItem requireOwnStockRecord(Seller seller, InventoryItem inventoryItem) {
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        Product product = productRepositoryPort.findById(inventoryItem.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("product", inventoryItem.getProductId()));
        Warehouse warehouse = warehouseRepositoryPort.findById(inventoryItem.getWarehouseId())
                .orElseThrow(() -> new EntityNotFoundException("warehouse", inventoryItem.getWarehouseId()));
        validateSellerOwnershipService.validateStockRecord(seller, inventoryItem, product, warehouse);
        return inventoryItem;
    }
}
