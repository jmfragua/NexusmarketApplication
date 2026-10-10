package application.infrastructure.security;

import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Seller;
import application.domain.models.User;

/**
 * Narrows the authenticated user to the concrete specialization an entry port demands.
 *
 * <p>Several port methods take a {@code Buyer} or a {@code Seller} rather than a generic
 * {@code User<?>}, because the operation only makes sense for that participant. The authenticated
 * principal carries the real subtype — the persistence mapper rebuilt it from the stored user type —
 * so this is a checked narrowing, not a conversion.</p>
 */
public final class ActorResolver {

    private ActorResolver() {
    }

    /**
     * @throws UnauthorizedOperationException when the authenticated user is not a buyer.
     */
    public static Buyer requireBuyer(User<?> actor) {
        if (actor instanceof Buyer buyer) {
            return buyer;
        }
        throw new UnauthorizedOperationException(
                "the authenticated user is not a buyer and cannot execute buyer operations");
    }

    /**
     * @throws UnauthorizedOperationException when the authenticated user is not a seller.
     */
    public static Seller requireSeller(User<?> actor) {
        if (actor instanceof Seller seller) {
            return seller;
        }
        throw new UnauthorizedOperationException(
                "the authenticated user is not a seller and cannot execute seller operations");
    }
}
