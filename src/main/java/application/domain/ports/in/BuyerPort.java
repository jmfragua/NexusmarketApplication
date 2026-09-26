package application.domain.ports.in;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.Product;
import application.domain.models.ProductReturn;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import java.util.List;

/**
 * Entry contract of the buyer role.
 *
 * <p>A buyer manages only the information needed to take part in commercial processes: their own
 * addresses, their own cart, their own orders and the returns derived from them. They never manage
 * information of other buyers nor inventories (RD-ROL-04, RD-PED-05).</p>
 */
public interface BuyerPort {

    // Profile and delivery locations

    Buyer addAdditionalAddress(Buyer buyer, Address address);

    Buyer changePrimaryAddress(Buyer buyer, Address address);

    // Cart: provisional selection, commits neither inventory nor invoicing (RD-PED-04)

    Cart consultMyCart(Buyer buyer);

    CartItem addCartItem(Buyer buyer, Cart cart, Product product, ProductVariantId variantId, Quantity quantity,
                         CartItemId cartItemId);

    Cart removeCartItem(Buyer buyer, Cart cart, CartItem item);

    Cart updateCartItemQuantity(Buyer buyer, Cart cart, CartItem item, Quantity quantity);

    Cart clearCart(Buyer buyer, Cart cart);

    // Orders

    /**
     * Confirms the cart as an order, first transition of the sequential cycle (RD-PED-01).
     */
    Order confirmCart(Buyer buyer, Cart cart, OrderId orderId, Address deliveryAddress, List<OrderLine> lines);

    Order confirmPayment(Buyer buyer, Order order);

    Order consultMyOrder(Buyer buyer, OrderId orderId);

    List<Order> consultMyOrders(Buyer buyer);

    Invoice consultMyInvoice(Buyer buyer, Order order);

    List<Invoice> consultMyInvoices(Buyer buyer);

    // After sales

    ProductReturn requestReturn(Buyer buyer, Order order, OrderLine orderLine, Quantity quantity,
                                ProductReturnId returnId);

    List<ProductReturn> consultMyReturns(Buyer buyer, Order order);
}
