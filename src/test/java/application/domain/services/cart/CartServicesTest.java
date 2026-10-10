package application.domain.services.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.BuyerNotAllowedToPurchaseException;
import application.domain.exceptions.EmptyCartException;
import application.domain.exceptions.ProductNotAvailableException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.PhysicalProduct;
import application.domain.models.Seller;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exercises the cart services, whose defining property is that the cart commits nothing: it reserves
 * no stock and generates no invoicing until it is confirmed (RD-PED-04).
 */
class CartServicesTest {

    private ServiceHarness harness;

    private Buyer buyer;

    private Seller seller;

    private Cart cart;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        buyer = DomainFixtures.buyer();
        seller = DomainFixtures.seller();
        cart = DomainFixtures.emptyCart(buyer);
    }

    private PhysicalProduct sellableProduct() {
        return DomainFixtures.sellableProduct(seller);
    }

    @Nested
    @DisplayName("Add cart item")
    class AddItem {

        @Test
        @DisplayName("selects a published product with variants")
        void selectsSellableProduct() {
            harness.addCartItem().addItem(buyer, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));

            assertEquals(1, cart.getItems().size());
            assertEquals(2, cart.getItems().get(0).getQuantity().getValue());
            assertEquals(0, harness.inventory.storedMovements(), "the cart commits no stock (RD-PED-04)");
        }

        @Test
        @DisplayName("accumulates the quantity instead of duplicating the line")
        void accumulatesSameVariant() {
            PhysicalProduct product = sellableProduct();
            harness.addCartItem().addItem(buyer, cart, product, ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));
            harness.addCartItem().addItem(buyer, cart, product, ProductVariantId.of("variant-1"),
                    Quantity.of(3), CartItemId.of("item-2"));

            assertEquals(1, cart.getItems().size());
            assertEquals(5, cart.getItems().get(0).getQuantity().getValue());
        }

        @Test
        @DisplayName("rejects a product without variants, which cannot be sold (RD-CAT-05)")
        void rejectsProductWithoutVariants() {
            assertThrows(ProductNotAvailableException.class, () -> harness.addCartItem()
                    .addItem(buyer, cart, DomainFixtures.productWithoutVariants(seller),
                            ProductVariantId.of("variant-1"), Quantity.of(1), CartItemId.of("item-1")));
        }

        @Test
        @DisplayName("rejects a suspended product, which is not visible in the catalogue (RD-CAT-03)")
        void rejectsSuspendedProduct() {
            assertThrows(ProductNotAvailableException.class, () -> harness.addCartItem()
                    .addItem(buyer, cart, DomainFixtures.productInStatus(seller, ProductStatus.SUSPENDED),
                            ProductVariantId.of("variant-1"), Quantity.of(1), CartItemId.of("item-1")));
        }

        @Test
        @DisplayName("rejects a cart that belongs to another buyer")
        void rejectsForeignCart() {
            Buyer intruder = DomainFixtures.buyer("buyer-2", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);

            assertThrows(UnauthorizedOperationException.class, () -> harness.addCartItem()
                    .addItem(intruder, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                            Quantity.of(1), CartItemId.of("item-1")));
        }
    }

    @Nested
    @DisplayName("Clear cart")
    class Clear {

        @Test
        @DisplayName("empties the selection without releasing anything")
        void emptiesSelection() {
            harness.addCartItem().addItem(buyer, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));

            harness.clearCart().clear(buyer, cart);

            assertTrue(cart.isEmpty());
            assertEquals(0, harness.inventory.storedMovements());
        }
    }

    @Nested
    @DisplayName("Confirm cart")
    class Confirm {

        private List<OrderLine> matchingLines(OrderId orderId) {
            return List.of(new OrderLine(
                    OrderLineId.of("order-line-1"),
                    orderId,
                    sellableProduct().getIdentifier(),
                    ProductVariantId.of("variant-1"),
                    Quantity.of(2),
                    DomainFixtures.money(20000)));
        }

        @Test
        @DisplayName("produces an order waiting for payment and empties the cart")
        void producesPendingOrder() {
            harness.addCartItem().addItem(buyer, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));
            OrderId orderId = OrderId.of("order-1");

            Order order = harness.confirmCart()
                    .confirm(buyer, cart, orderId, buyer.getPrimaryAddress(), matchingLines(orderId));

            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            assertTrue(cart.isEmpty(), "the selection was committed, so the cart is emptied");
        }

        @Test
        @DisplayName("rejects an empty cart")
        void rejectsEmptyCart() {
            OrderId orderId = OrderId.of("order-1");

            assertThrows(EmptyCartException.class, () -> harness.confirmCart()
                    .confirm(buyer, cart, orderId, buyer.getPrimaryAddress(), matchingLines(orderId)));
        }

        @Test
        @DisplayName("rejects a commercially disabled buyer even when active on the platform")
        void rejectsDisabledBuyer() {
            Buyer disabled = DomainFixtures.buyer("buyer-3", UserStatus.ACTIVE, BuyerCommercialStatus.DISABLED);
            Cart disabledCart = DomainFixtures.emptyCart(disabled);
            OrderId orderId = OrderId.of("order-1");

            assertThrows(BuyerNotAllowedToPurchaseException.class, () -> harness.confirmCart()
                    .confirm(disabled, disabledCart, orderId, disabled.getPrimaryAddress(),
                            matchingLines(orderId)));
        }

        @Test
        @DisplayName("rejects a delivery address the buyer has not registered")
        void rejectsUnknownAddress() {
            harness.addCartItem().addItem(buyer, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));
            OrderId orderId = OrderId.of("order-1");

            assertThrows(IllegalArgumentException.class, () -> harness.confirmCart()
                    .confirm(buyer, cart, orderId, Address.of("Somewhere never registered"),
                            matchingLines(orderId)));
        }

        @Test
        @DisplayName("rejects lines that do not match the selection one to one")
        void rejectsMismatchedLines() {
            harness.addCartItem().addItem(buyer, cart, sellableProduct(), ProductVariantId.of("variant-1"),
                    Quantity.of(2), CartItemId.of("item-1"));
            OrderId orderId = OrderId.of("order-1");
            List<OrderLine> wrongQuantity = List.of(new OrderLine(
                    OrderLineId.of("order-line-1"), orderId,
                    sellableProduct().getIdentifier(), ProductVariantId.of("variant-1"),
                    Quantity.of(99), DomainFixtures.money(20000)));

            assertThrows(IllegalArgumentException.class, () -> harness.confirmCart()
                    .confirm(buyer, cart, orderId, buyer.getPrimaryAddress(), wrongQuantity));
        }
    }
}
