package application.domain.models;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Objects;

/**
 * Physical product. Needs stock in inventory and a logistic dispatch to be delivered.
 */
public class PhysicalProduct extends Product {

    public PhysicalProduct(ProductId identifier, SellerId sellerId) {
        super(identifier, sellerId, ProductType.PHYSICAL);
    }

    private PhysicalProduct(ProductId identifier,
                            SellerId sellerId,
                            ProductStatus status,
                            List<ProductVariant> variants) {
        super(identifier, sellerId, ProductType.PHYSICAL, status, variants);
    }

    /**
     * Restores a physical product that already existed, with the state and variants it had been
     * persisted in (RD-ALC-04).
     */
    public static PhysicalProduct reconstruct(ProductId identifier,
                                              SellerId sellerId,
                                              ProductStatus status,
                                              List<ProductVariant> variants) {
        return new PhysicalProduct(identifier, sellerId, status, variants);
    }

    @Override
    public boolean requiresInventory() {
        return true;
    }

    /**
     * @return the stock of this product available for reservation in the given warehouse, zero when
     *         the product has no stock record there.
     */
    public Quantity availableStockIn(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse is mandatory");
        return warehouse.stockOf(getIdentifier())
                .map(InventoryItem::getAvailableQuantity)
                .orElseGet(Quantity::zero);
    }
}
