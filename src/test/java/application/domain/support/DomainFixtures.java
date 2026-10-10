package application.domain.support;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.InventoryItem;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.PhysicalProduct;
import application.domain.models.ProductVariant;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.VariantAttribute;
import application.domain.valuesObjects.WarehouseId;
import java.math.BigDecimal;
import java.util.List;

/**
 * Builders of valid domain objects for the tests.
 *
 * <p>Everything built here satisfies the invariants of the model, so a test that fails does so
 * because of the rule under exam, never because the fixture was malformed.</p>
 */
public final class DomainFixtures {

    public static final String CURRENCY = "COP";

    private DomainFixtures() {
    }

    public static Buyer buyer() {
        return buyer("buyer-1", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);
    }

    public static Buyer buyer(String id, UserStatus status, BuyerCommercialStatus commercialStatus) {
        return new Buyer(
                BuyerId.of(id),
                FullName.of("Ana Buyer"),
                EmailAddress.of(id + "@nexusmarket.test"),
                IdentityDocument.of("doc-" + id),
                status,
                Address.of("Calle 1 #2-3"),
                commercialStatus);
    }

    public static Seller seller() {
        return seller("seller-1");
    }

    public static Seller seller(String id) {
        return Seller.reconstruct(
                SellerId.of(id),
                FullName.of("Carlos Seller"),
                EmailAddress.of(id + "@nexusmarket.test"),
                IdentityDocument.of("doc-" + id),
                UserStatus.ACTIVE);
    }

    /**
     * Builds a user of a role that owns no specialization in the model — logistics operator,
     * administrator or supervisor — which the specification represents through {@code UserRole}
     * alone.
     */
    public static User<UserId> userWithRole(String id, UserRole role) {
        return new User<>(UserId.of(id),
                FullName.of("Staff " + id),
                EmailAddress.of(id + "@nexusmarket.test"),
                IdentityDocument.of("doc-" + id),
                role,
                UserStatus.ACTIVE) {

            @Override
            protected boolean ownsResource(application.domain.models.DomainEntity<?> resource) {
                return true;
            }
        };
    }

    public static User<UserId> logisticsOperator() {
        return userWithRole("logistics-1", UserRole.LOGISTICS_OPERATOR);
    }

    public static User<UserId> administrator() {
        return userWithRole("admin-1", UserRole.ADMINISTRATOR);
    }

    public static User<UserId> supervisor() {
        return userWithRole("supervisor-1", UserRole.SUPERVISOR);
    }

    public static SellerWarehouse sellerWarehouse(Seller seller) {
        return new SellerWarehouse(WarehouseId.of("warehouse-1"), seller.getIdentifier());
    }

    /**
     * A published physical product holding one variant, which is what makes it visible in the
     * catalogue (RD-CAT-03, RD-CAT-05).
     */
    public static PhysicalProduct sellableProduct(Seller seller) {
        PhysicalProduct product = new PhysicalProduct(ProductId.of("product-1"), seller.getIdentifier());
        product.addVariant(variant(product.getIdentifier()));
        return product;
    }

    public static PhysicalProduct productWithoutVariants(Seller seller) {
        return new PhysicalProduct(ProductId.of("product-2"), seller.getIdentifier());
    }

    public static PhysicalProduct productInStatus(Seller seller, ProductStatus status) {
        PhysicalProduct product = PhysicalProduct.reconstruct(
                ProductId.of("product-3"), seller.getIdentifier(), status, List.of());
        return product;
    }

    public static ProductVariant variant(ProductId productId) {
        return new ProductVariant(
                ProductVariantId.of("variant-1"),
                productId,
                List.of(VariantAttribute.of("color", "red")));
    }

    public static Cart emptyCart(Buyer buyer) {
        return new Cart(CartId.of("cart-1"), buyer.getIdentifier());
    }

    public static InventoryItem emptyStock() {
        return new InventoryItem(
                InventoryItemId.of("stock-1"),
                ProductId.of("product-1"),
                WarehouseId.of("warehouse-1"));
    }

    /**
     * A stock record restored with balances and condition, without replaying the commands that would
     * have produced them (RD-ALC-04).
     */
    public static InventoryItem stockWith(int available, int reserved, StockCondition condition) {
        return InventoryItem.reconstruct(
                InventoryItemId.of("stock-1"),
                ProductId.of("product-1"),
                WarehouseId.of("warehouse-1"),
                Quantity.of(available),
                Quantity.of(reserved),
                condition,
                List.of());
    }

    public static Order orderIn(Buyer buyer, OrderStatus status) {
        OrderId orderId = OrderId.of("order-1");
        return Order.reconstruct(
                orderId,
                buyer.getIdentifier(),
                buyer.getPrimaryAddress(),
                List.of(orderLine(orderId)),
                status);
    }

    public static OrderLine orderLine(OrderId orderId) {
        return new OrderLine(
                OrderLineId.of("order-line-1"),
                orderId,
                ProductId.of("product-1"),
                ProductVariantId.of("variant-1"),
                Quantity.of(2),
                money(20000));
    }

    public static Money money(long amount) {
        return Money.of(BigDecimal.valueOf(amount), CURRENCY);
    }
}
