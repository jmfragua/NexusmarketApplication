package application.domain.services.authorization;

import application.domain.exceptions.ProductNotOwnedException;
import application.domain.exceptions.WarehouseNotOwnedException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Warehouse;
import java.util.Objects;

/**
 * Verifies that the product or warehouse the seller acts upon is their own.
 *
 * <p>Every product belongs to a single seller and only that seller manages it (RD-CAT-01), and the
 * same applies to the warehouses of the seller (RG-03, RD-ROL-03).</p>
 */
public class ValidateSellerOwnershipService {

    /**
     * @throws ProductNotOwnedException when the product was registered under another seller.
     */
    public void validateProduct(Seller seller, Product product) {
        Objects.requireNonNull(seller, "seller is mandatory");
        Objects.requireNonNull(product, "product is mandatory");
        if (!seller.ownsProduct(product)) {
            throw new ProductNotOwnedException(seller.getIdentifier(), product.getIdentifier());
        }
    }

    /**
     * @throws WarehouseNotOwnedException when the warehouse belongs to another seller or to the
     *                                    marketplace.
     */
    public void validateWarehouse(Seller seller, Warehouse warehouse) {
        Objects.requireNonNull(seller, "seller is mandatory");
        Objects.requireNonNull(warehouse, "warehouse is mandatory");
        if (!seller.ownsWarehouse(warehouse)) {
            throw new WarehouseNotOwnedException(seller.getIdentifier(), warehouse.getIdentifier());
        }
    }
}
