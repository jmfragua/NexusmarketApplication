package application.domain.services.cart;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Empties the provisional selection of the buyer.
 */
public class ClearCartService {

    private static final String OPERATION = "clear cart";

    private final CartRepositoryPort cartRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public ClearCartService(CartRepositoryPort cartRepositoryPort,
                            ValidateRoleAuthorizationService validateRoleAuthorizationService,
                            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    public Cart clear(Buyer buyer, Cart cart) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateCart(buyer, cart);
        cart.clear();
        return cartRepositoryPort.save(cart);
    }
}
