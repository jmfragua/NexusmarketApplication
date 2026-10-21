package application.domain.ports.out;

import application.domain.models.Cart;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.CartItemId;
import java.util.Optional;

/**
 * Output port of the provisional selection of a buyer. A buyer holds one active cart (RD-PED-04).
 */
public interface CartRepositoryPort {

    Cart save(Cart cart);

    /**
     * Stores a new cart. Unlike {@link #save}, it never replaces an existing one: when the
     * identifier is already taken the storage refuses it, so a creation can never act as an update
     * (RD-ID-06).
     */
    Cart create(Cart cart);

    Optional<Cart> findById(CartId cartId);

    Optional<Cart> findByBuyer(BuyerId buyerId);

    /**
     * @return whether any cart already holds a line with this identifier.
     */
    boolean existsCartItem(CartItemId cartItemId);
}
