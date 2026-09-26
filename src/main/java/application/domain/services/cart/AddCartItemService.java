package application.domain.services.cart;

import application.domain.exceptions.ProductNotAvailableException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Product;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Selects a product variant into the cart of a buyer.
 *
 * <p>The cart is a provisional selection: it commits neither inventory nor invoicing (RD-PED-04).
 * Only a product visible in the public catalogue can be selected (RD-CAT-03), and the variant is
 * the unit of commercial selection (RD-CAT-05).</p>
 */
public class AddCartItemService {

    private static final String OPERATION = "add item to cart";

    private final CartRepositoryPort cartRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public AddCartItemService(CartRepositoryPort cartRepositoryPort,
                              ValidateRoleAuthorizationService validateRoleAuthorizationService,
                              ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.validateBuyerOwnershipService = Objects.requireNonNull(validateBuyerOwnershipService,
                "the ownership validation is mandatory");
    }

    /**
     * @param product   product being selected, which must be visible in the public catalogue.
     * @param variantId variant of that product chosen by the buyer.
     * @return the resulting cart line; selecting an already present variant accumulates the units.
     * @throws ProductNotAvailableException when the product is not available for sale.
     */
    public CartItem addItem(Buyer buyer,
                            Cart cart,
                            Product product,
                            ProductVariantId variantId,
                            Quantity quantity,
                            CartItemId cartItemId) {
        validateRoleAuthorizationService.validate(buyer, OPERATION, UserRole.BUYER);
        validateBuyerOwnershipService.validateCart(buyer, cart);
        Objects.requireNonNull(product, "product is mandatory");
        if (!product.isVisibleInCatalog()) {
            throw new ProductNotAvailableException(product.getIdentifier());
        }
        CartItem item = cart.addItem(cartItemId, product.getIdentifier(), variantId, quantity);
        cartRepositoryPort.save(cart);
        return item;
    }
}
