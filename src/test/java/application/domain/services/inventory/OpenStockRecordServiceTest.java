package application.domain.services.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.DuplicateInventoryItemException;
import application.domain.exceptions.ProductNotOwnedException;
import application.domain.exceptions.StockRecordNotAllowedException;
import application.domain.exceptions.UserNotActiveException;
import application.domain.exceptions.WarehouseNotOwnedException;
import application.domain.models.DigitalProduct;
import application.domain.models.InventoryItem;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.PhysicalProduct;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.WarehouseId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Opening a stock record: one record per product and warehouse (RD-INV-01, RD-ID-05), only for the
 * seller's own physical products in their own warehouses (RD-CAT-01, RG-03, RD-INV-05).
 */
class OpenStockRecordServiceTest {

    private ServiceHarness harness;

    private Seller seller;

    private PhysicalProduct product;

    private SellerWarehouse warehouse;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        seller = DomainFixtures.seller();
        product = DomainFixtures.sellableProduct(seller);
        warehouse = DomainFixtures.sellerWarehouse(seller);
    }

    private InventoryItem open(String id) {
        return harness.openStockRecord().open(seller, product, warehouse, InventoryItemId.of(id));
    }

    @Test
    @DisplayName("opens an empty, available record binding the product to the warehouse")
    void opensEmptyRecord() {
        InventoryItem record = open("stock-new");

        assertEquals(product.getIdentifier(), record.getProductId());
        assertEquals(warehouse.getIdentifier(), record.getWarehouseId());
        assertEquals(0, record.getAvailableQuantity().getValue());
        assertEquals(0, record.getReservedQuantity().getValue());
        assertEquals(StockCondition.AVAILABLE, record.getCondition());
        assertTrue(record.getMovements().isEmpty(), "opening a record changes no stock, so it has no movement");
        assertTrue(harness.inventory.items().findById(InventoryItemId.of("stock-new")).isPresent());
    }

    @Test
    @DisplayName("rejects a second record for the same product in the same warehouse (RD-ID-05)")
    void rejectsDuplicatedPair() {
        open("stock-a");

        assertThrows(DuplicateInventoryItemException.class, () -> open("stock-b"));
    }

    @Test
    @DisplayName("rejects an identifier already taken, which would overwrite the existing record")
    void rejectsTakenIdentifier() {
        open("stock-a");
        PhysicalProduct other = new PhysicalProduct(ProductId.of("product-other"), seller.getIdentifier());

        assertThrows(DuplicateInventoryItemException.class, () -> harness.openStockRecord()
                .open(seller, other, warehouse, InventoryItemId.of("stock-a")));
    }

    @Test
    @DisplayName("rejects the product of another seller (RD-CAT-01)")
    void rejectsForeignProduct() {
        PhysicalProduct foreign = new PhysicalProduct(ProductId.of("product-x"), SellerId.of("seller-2"));

        assertThrows(ProductNotOwnedException.class, () -> harness.openStockRecord()
                .open(seller, foreign, warehouse, InventoryItemId.of("stock-x")));
    }

    @Test
    @DisplayName("rejects a warehouse of another seller and a marketplace warehouse (RG-03)")
    void rejectsForeignWarehouse() {
        SellerWarehouse foreign = new SellerWarehouse(WarehouseId.of("warehouse-x"), SellerId.of("seller-2"));
        MarketplaceWarehouse marketplace = new MarketplaceWarehouse(WarehouseId.of("warehouse-m"));

        assertThrows(WarehouseNotOwnedException.class, () -> harness.openStockRecord()
                .open(seller, product, foreign, InventoryItemId.of("stock-x")));
        assertThrows(WarehouseNotOwnedException.class, () -> harness.openStockRecord()
                .open(seller, product, marketplace, InventoryItemId.of("stock-m")));
    }

    @Test
    @DisplayName("rejects a digital product, which never holds stock (RD-INV-05)")
    void rejectsDigitalProduct() {
        DigitalProduct digital = new DigitalProduct(ProductId.of("product-d"), seller.getIdentifier());

        assertThrows(StockRecordNotAllowedException.class, () -> harness.openStockRecord()
                .open(seller, digital, warehouse, InventoryItemId.of("stock-d")));
    }

    @Test
    @DisplayName("rejects a discontinued product (RD-CAT-04)")
    void rejectsDiscontinuedProduct() {
        PhysicalProduct discontinued = DomainFixtures.productInStatus(seller, ProductStatus.DISCONTINUED);

        assertThrows(StockRecordNotAllowedException.class, () -> harness.openStockRecord()
                .open(seller, discontinued, warehouse, InventoryItemId.of("stock-z")));
    }

    @Test
    @DisplayName("rejects a blocked seller before touching anything (RG-01)")
    void rejectsBlockedSeller() {
        Seller blocked = Seller.reconstruct(seller.getIdentifier(), FullName.of("Blocked"),
                EmailAddress.of("blocked@nexusmarket.test"), IdentityDocument.of("doc-blocked"), UserStatus.BLOCKED);

        assertThrows(UserNotActiveException.class, () -> harness.openStockRecord()
                .open(blocked, product, warehouse, InventoryItemId.of("stock-b")));
        assertTrue(harness.inventory.items().findAll().isEmpty());
    }
}
