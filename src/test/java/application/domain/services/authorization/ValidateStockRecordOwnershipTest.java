package application.domain.services.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import application.domain.exceptions.ProductNotOwnedException;
import application.domain.exceptions.WarehouseNotOwnedException;
import application.domain.models.InventoryItem;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.PhysicalProduct;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.support.DomainFixtures;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A stock record is the seller's only when its product and its warehouse both are (RD-CAT-01, RG-03).
 */
class ValidateStockRecordOwnershipTest {

    private final ValidateSellerOwnershipService validator = new ValidateSellerOwnershipService();

    private final Seller seller = DomainFixtures.seller();

    private final PhysicalProduct ownProduct = new PhysicalProduct(ProductId.of("p-own"), seller.getIdentifier());

    private final SellerWarehouse ownWarehouse = new SellerWarehouse(WarehouseId.of("wh-own"), seller.getIdentifier());

    private static InventoryItem record(String productId, String warehouseId) {
        return new InventoryItem(InventoryItemId.of("inv"), ProductId.of(productId), WarehouseId.of(warehouseId));
    }

    @Test
    @DisplayName("accepts a record of an own product in an own warehouse")
    void ownRecord() {
        assertDoesNotThrow(() -> validator.validateStockRecord(seller, record("p-own", "wh-own"), ownProduct, ownWarehouse));
    }

    @Test
    @DisplayName("rejects a record of another seller's product")
    void foreignProduct() {
        PhysicalProduct foreign = new PhysicalProduct(ProductId.of("p-x"), SellerId.of("seller-x"));

        assertThrows(ProductNotOwnedException.class,
                () -> validator.validateStockRecord(seller, record("p-x", "wh-own"), foreign, ownWarehouse));
    }

    @Test
    @DisplayName("rejects a record in another seller's warehouse or in a marketplace warehouse")
    void foreignWarehouse() {
        SellerWarehouse foreign = new SellerWarehouse(WarehouseId.of("wh-x"), SellerId.of("seller-x"));
        MarketplaceWarehouse marketplace = new MarketplaceWarehouse(WarehouseId.of("wh-m"));

        assertThrows(WarehouseNotOwnedException.class,
                () -> validator.validateStockRecord(seller, record("p-own", "wh-x"), ownProduct, foreign));
        assertThrows(WarehouseNotOwnedException.class,
                () -> validator.validateStockRecord(seller, record("p-own", "wh-m"), ownProduct, marketplace));
    }

    @Test
    @DisplayName("refuses a product or warehouse that is not the one the record points to")
    void mismatchedResolution() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateStockRecord(seller, record("p-other", "wh-own"), ownProduct, ownWarehouse));
    }
}
