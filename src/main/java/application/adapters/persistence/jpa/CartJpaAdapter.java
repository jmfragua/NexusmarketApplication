package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaCartRepository;
import application.domain.models.Cart;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link CartRepositoryPort}.
 */
@Repository
public class CartJpaAdapter implements CartRepositoryPort {

    private final SpringDataJpaCartRepository cartRepository;

    public CartJpaAdapter(SpringDataJpaCartRepository cartRepository) {
        this.cartRepository = Objects.requireNonNull(cartRepository, "the cart repository is mandatory");
    }

    @Override
    public Cart save(Cart cart) {
        cartRepository.save(CartJpaMapper.toEntity(cart));
        return cart;
    }

    @Override
    public Optional<Cart> findById(CartId cartId) {
        return cartRepository.findById(cartId.getValue()).map(CartJpaMapper::toDomain);
    }

    @Override
    public Optional<Cart> findByBuyer(BuyerId buyerId) {
        return cartRepository.findByBuyerId(buyerId.getValue()).map(CartJpaMapper::toDomain);
    }
}
