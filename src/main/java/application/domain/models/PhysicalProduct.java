package application.domain.models;

import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.SellerId;
import java.util.Objects;

/**
 * Physical product. Needs stock in inventory and a logistic dispatch to be delivered.
 */
public class PhysicalProduct extends Product {

    public PhysicalProduct(ProductId identifier, SellerId sellerId) {
        super(identifier, sellerId, ProductType.PHYSICAL);
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
