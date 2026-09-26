package application.domain.ports.in;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.Warehouse;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;

/**
 * Entry contract of the seller role.
 *
 * <p>A seller registers and manages their own products (RD-CAT-01) and shares the administration of
 * the inventory with the logistics operator (RD-ROL-06). Every operation is limited to the products
 * and warehouses they own (RG-03).</p>
 */
public interface SellerPort {

    // Own catalogue

    Product registerProduct(Seller seller, Product product);

    Product publishProduct(Seller seller, Product product);

    Product suspendProduct(Seller seller, Product product);

    Product discontinueProduct(Seller seller, Product product);

    Product addProductVariant(Seller seller, Product product, ProductVariant variant);

    Product removeProductVariant(Seller seller, Product product, ProductVariant variant);

    List<Product> consultMyCatalog(Seller seller);

    // Own warehouses

    List<SellerWarehouse> consultMyWarehouses(Seller seller);

    Warehouse consultMyWarehouse(Seller seller, WarehouseId warehouseId);

    // Inventory of the own products

    InventoryMovement receiveStock(Seller seller, InventoryItem inventoryItem, Quantity quantity,
                                   InventoryMovementId movementId);

    InventoryMovement adjustStock(Seller seller, InventoryItem inventoryItem, Quantity newAvailableQuantity,
                                  InventoryMovementId movementId);

    InventoryItem markStockAsDamaged(Seller seller, InventoryItem inventoryItem);

    List<InventoryItem> consultStockOfMyProduct(Seller seller, ProductId productId);
}
