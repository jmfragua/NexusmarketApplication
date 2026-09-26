package application.domain.services.cart;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Withdraws a line from the provisional selection of the buyer.
 *
 * <p>The cart commits no inventory, so removing a line frees nothing: nothing was reserved
 * (RD-PED-04).</p>
 */
public class RemoveCartItemService {

    private static final String OPERATION = "remove item from cart";

    private final CartRepositoryPort cartRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public RemoveCartItemService(CartRepositoryPort cartRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Cart removeItem(Buyer buyer, Cart cart, CartItem item) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateCart(buyer, cart);
        cart.removeItem(item);
        return cartRepositoryPort.save(cart);
    }
}
