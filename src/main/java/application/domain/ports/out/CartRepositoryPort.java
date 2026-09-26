package application.domain.ports.out;

import application.domain.models.Cart;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import java.util.Optional;

/**
 * Output port of the provisional selection of a buyer. A buyer holds one active cart (RD-PED-04).
 */
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findById(CartId cartId);

    Optional<Cart> findByBuyer(BuyerId buyerId);
}
