package application.domain.services.cart;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Changes the units selected in a line of the cart. A cart line can never be of zero units.
 */
public class UpdateCartItemQuantityService {

    private static final String OPERATION = "update cart item quantity";

    private final CartRepositoryPort cartRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public UpdateCartItemQuantityService(CartRepositoryPort cartRepositoryPort,
                                         ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                         ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Cart updateQuantity(Buyer buyer, Cart cart, CartItem item, Quantity quantity) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateCart(buyer, cart);
        cart.updateQuantity(item, quantity);
        return cartRepositoryPort.save(cart);
    }
}
