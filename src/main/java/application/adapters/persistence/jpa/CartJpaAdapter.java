package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaCartRepository;
import application.domain.models.Cart;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.CartItemId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link CartRepositoryPort}.
 */
@Repository
public class CartJpaAdapter implements CartRepositoryPort {

    private final SpringDataJpaCartRepository cartRepository;

    private final JpaInserter jpaInserter;

    public CartJpaAdapter(SpringDataJpaCartRepository cartRepository,
                          JpaInserter jpaInserter) {
        this.cartRepository = Objects.requireNonNull(cartRepository, "the cart repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public Cart save(Cart cart) {
        cartRepository.save(CartJpaMapper.toEntity(cart));
        return cart;
    }

    @Override
    public Cart create(Cart cart) {
        jpaInserter.insert(CartJpaMapper.toEntity(cart));
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

    @Override
    public boolean existsCartItem(CartItemId cartItemId) {
        return cartRepository.existsByItemsId(cartItemId.getValue());
    }
}
