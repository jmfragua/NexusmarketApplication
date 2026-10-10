package application.adapters.useCases;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.Product;
import application.domain.models.ProductReturn;
import application.domain.ports.in.BuyerPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.cart.AddCartItemService;
import application.domain.services.cart.ClearCartService;
import application.domain.services.cart.ConfirmCartService;
import application.domain.services.cart.RemoveCartItemService;
import application.domain.services.cart.UpdateCartItemQuantityService;
import application.domain.services.invoice.ConsultInvoiceService;
import application.domain.services.order.ConfirmPaymentService;
import application.domain.services.order.ConsultOrderService;
import application.domain.services.returns.ConsultReturnService;
import application.domain.services.returns.RequestReturnService;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implements the buyer entry port by delegating to the domain services.
 *
 * <p>Three operations have no domain service behind them — managing the own addresses and reading
 * the own cart — because they are thin entity mutations that carry no rule of their own beyond
 * ownership. They are orchestrated here against the output ports directly, still passing through
 * the role validation every other operation goes through (RG-01, RG-03).</p>
 */
@Service
public class BuyerUseCaseImpl implements BuyerPort {

    private static final String ADDRESS_OPERATION = "manage own delivery addresses";

    private static final String CART_OPERATION = "consult own cart";

    private final UserRepositoryPort userRepositoryPort;

    private final CartRepositoryPort cartRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final AddCartItemService addCartItemService;

    private final RemoveCartItemService removeCartItemService;

    private final UpdateCartItemQuantityService updateCartItemQuantityService;

    private final ClearCartService clearCartService;

    private final ConfirmCartService confirmCartService;

    private final ConfirmPaymentService confirmPaymentService;

    private final ConsultOrderService consultOrderService;

    private final ConsultInvoiceService consultInvoiceService;

    private final RequestReturnService requestReturnService;

    private final ConsultReturnService consultReturnService;

    public BuyerUseCaseImpl(UserRepositoryPort userRepositoryPort,
                            CartRepositoryPort cartRepositoryPort,
                            ValidateRoleAuthorizationService validateRoleAuthorizationService,
                            AddCartItemService addCartItemService,
                            RemoveCartItemService removeCartItemService,
                            UpdateCartItemQuantityService updateCartItemQuantityService,
                            ClearCartService clearCartService,
                            ConfirmCartService confirmCartService,
                            ConfirmPaymentService confirmPaymentService,
                            ConsultOrderService consultOrderService,
                            ConsultInvoiceService consultInvoiceService,
                            RequestReturnService requestReturnService,
                            ConsultReturnService consultReturnService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort);
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort);
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService);
        this.addCartItemService = Objects.requireNonNull(addCartItemService);
        this.removeCartItemService = Objects.requireNonNull(removeCartItemService);
        this.updateCartItemQuantityService = Objects.requireNonNull(updateCartItemQuantityService);
        this.clearCartService = Objects.requireNonNull(clearCartService);
        this.confirmCartService = Objects.requireNonNull(confirmCartService);
        this.confirmPaymentService = Objects.requireNonNull(confirmPaymentService);
        this.consultOrderService = Objects.requireNonNull(consultOrderService);
        this.consultInvoiceService = Objects.requireNonNull(consultInvoiceService);
        this.requestReturnService = Objects.requireNonNull(requestReturnService);
        this.consultReturnService = Objects.requireNonNull(consultReturnService);
    }

    @Override
    public Buyer addAdditionalAddress(Buyer buyer, Address address) {
        validateRoleAuthorizationService.validate(buyer, ADDRESS_OPERATION, UserRole.BUYER);
        buyer.addAdditionalAddress(address);
        return userRepositoryPort.save(buyer);
    }

    @Override
    public Buyer changePrimaryAddress(Buyer buyer, Address address) {
        validateRoleAuthorizationService.validate(buyer, ADDRESS_OPERATION, UserRole.BUYER);
        buyer.changePrimaryAddress(address);
        return userRepositoryPort.save(buyer);
    }

    @Override
    public Cart consultMyCart(Buyer buyer) {
        validateRoleAuthorizationService.validate(buyer, CART_OPERATION, UserRole.BUYER);
        return cartRepositoryPort.findByBuyer(buyer.getIdentifier())
                .orElseThrow(() -> new EntityNotFoundException(
                        "buyer " + buyer.getIdentifier() + " holds no active cart"));
    }

    @Override
    public CartItem addCartItem(Buyer buyer, Cart cart, Product product, ProductVariantId variantId,
                                Quantity quantity, CartItemId cartItemId) {
        return addCartItemService.addItem(buyer, cart, product, variantId, quantity, cartItemId);
    }

    @Override
    public Cart removeCartItem(Buyer buyer, Cart cart, CartItem item) {
        return removeCartItemService.removeItem(buyer, cart, item);
    }

    @Override
    public Cart updateCartItemQuantity(Buyer buyer, Cart cart, CartItem item, Quantity quantity) {
        return updateCartItemQuantityService.updateQuantity(buyer, cart, item, quantity);
    }

    @Override
    public Cart clearCart(Buyer buyer, Cart cart) {
        return clearCartService.clear(buyer, cart);
    }

    @Override
    public Order confirmCart(Buyer buyer, Cart cart, OrderId orderId, Address deliveryAddress,
                             List<OrderLine> lines) {
        return confirmCartService.confirm(buyer, cart, orderId, deliveryAddress, lines);
    }

    @Override
    public Order confirmPayment(Buyer buyer, Order order) {
        return confirmPaymentService.confirmPayment(buyer, order);
    }

    @Override
    public Order consultMyOrder(Buyer buyer, OrderId orderId) {
        return consultOrderService.consultOwn(buyer, orderId);
    }

    @Override
    public List<Order> consultMyOrders(Buyer buyer) {
        return consultOrderService.consultOwn(buyer);
    }

    @Override
    public Invoice consultMyInvoice(Buyer buyer, Order order) {
        return consultInvoiceService.consultOwn(buyer, order);
    }

    @Override
    public List<Invoice> consultMyInvoices(Buyer buyer) {
        return consultInvoiceService.consultOwn(buyer);
    }

    @Override
    public ProductReturn requestReturn(Buyer buyer, Order order, OrderLine orderLine, Quantity quantity,
                                       ProductReturnId returnId) {
        return requestReturnService.requestReturn(buyer, order, orderLine, quantity, returnId);
    }

    @Override
    public List<ProductReturn> consultMyReturns(Buyer buyer, Order order) {
        return consultReturnService.consultOwn(buyer, order);
    }
}
