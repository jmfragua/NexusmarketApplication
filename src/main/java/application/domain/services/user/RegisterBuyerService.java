package application.domain.services.user;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.DuplicateUserIdentifierException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valuesObjects.CartId;
import java.util.Objects;

/**
 * Incorporates a buyer to the platform.
 *
 * <p>The email address and the identity document are unique across the whole platform. That
 * uniqueness depends on the data set and cannot be checked inside the value object (RD-VO-09), so
 * it is verified here, when the user joins the domain (RD-ID-03, RD-ID-04).</p>
 */
public class RegisterBuyerService {

    private final UserRepositoryPort userRepositoryPort;

    private final CartRepositoryPort cartRepositoryPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort, CartRepositoryPort cartRepositoryPort) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.cartRepositoryPort = Objects.requireNonNull(cartRepositoryPort, "the cart repository is mandatory");
    }

    /**
     * Incorporates the buyer together with their active cart.
     *
     * <p>The cart is created here, and not on demand later, because the model states that a buyer
     * holds one active cart (RD-PED-04): a buyer without one could never select a product, and the
     * whole purchase flow would be unreachable.</p>
     *
     * @param buyer  buyer already built and validated by the domain model.
     * @param cartId identifier assigned to the active cart of this buyer.
     * @return the registered buyer.
     * @throws DuplicateUserIdentifierException   when another user already holds the identifier (RD-ID-02).
     * @throws DuplicateEmailException            when the email address is already registered.
     * @throws DuplicateIdentityDocumentException when the identity document is already registered.
     */
    public Buyer register(Buyer buyer, CartId cartId) {
        Objects.requireNonNull(buyer, "buyer is mandatory");
        Objects.requireNonNull(cartId, "the identifier of the cart is mandatory");
        if (userRepositoryPort.findByIdentifier(buyer.getIdentifier()).isPresent()) {
            throw new DuplicateUserIdentifierException(buyer.getIdentifier());
        }
        if (userRepositoryPort.existsByEmail(buyer.getEmail())) {
            throw new DuplicateEmailException(buyer.getEmail());
        }
        if (userRepositoryPort.existsByIdentityDocument(buyer.getIdentityDocument())) {
            throw new DuplicateIdentityDocumentException(buyer.getIdentityDocument());
        }
        Buyer registered = userRepositoryPort.create(buyer);
        cartRepositoryPort.create(new Cart(cartId, registered.getIdentifier()));
        return registered;
    }
}
