package application.domain.services.cart;

import application.domain.exceptions.BuyerNotAllowedToPurchaseException;
import application.domain.exceptions.EmptyCartException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Confirms the provisional selection as an order, which takes it to {@code PENDING_PAYMENT}.
 *
 * <p>This is the first transition of the sequential cycle of the order (RD-PED-01). Only a buyer
 * whose commercial status allows purchases can confirm, and the delivery address must be one of the
 * registered locations of the buyer.</p>
 */
public class ConfirmCartService {

    private static final String OPERATION = "confirm cart";

    private final CartRepositoryPort cartRepositoryPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ConfirmCartService(CartRepositoryPort cartRepositoryPort,
                              OrderRepositoryPort orderRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService,
                              ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort, "the order repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @param deliveryAddress delivery location chosen among the registered ones of the buyer.
     * @param lines           priced lines, exactly one per cart line. The commercial value is
     *                        supplied by the caller because the specification models no price on the
     *                        product (RD-ALC-03).
     * @return the order, already in {@code PENDING_PAYMENT}.
     * @throws BuyerNotAllowedToPurchaseException when the commercial status blocks the purchase.
     * @throws EmptyCartException                 when the cart holds no lines.
     */
    public Order confirm(Buyer buyer, Cart cart, OrderId orderId, Address deliveryAddress, List<OrderLine> lines) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateCart(buyer, cart);
        if (!buyer.canPurchase()) {
            throw new BuyerNotAllowedToPurchaseException(buyer.getIdentifier());
        }
        if (cart.isEmpty()) {
            throw new EmptyCartException(cart.getIdentifier());
        }
        Order order = buyer.placeOrder(cart, orderId, deliveryAddress, lines);
        cartRepositoryPort.save(cart);
        return orderRepositoryPort.save(order);
    }
}
