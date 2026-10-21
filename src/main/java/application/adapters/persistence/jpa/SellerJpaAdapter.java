package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaUserRepository;
import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valuesObjects.SellerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link SellerRepositoryPort}.
 *
 * <p>Shares the {@code users} table with {@link UserJpaAdapter}, filtered by user type: a seller is
 * a specialization of user in the domain, and storing it twice would let both copies drift
 * apart.</p>
 */
@Repository
public class SellerJpaAdapter implements SellerRepositoryPort {

    private static final String SELLER_TYPE = "SELLER";

    private final SpringDataJpaUserRepository userRepository;

    private final JpaInserter jpaInserter;

    public SellerJpaAdapter(SpringDataJpaUserRepository userRepository,
                            JpaInserter jpaInserter) {
        this.userRepository = Objects.requireNonNull(userRepository, "the user repository is mandatory");
        this.jpaInserter = Objects.requireNonNull(jpaInserter, "the inserter is mandatory");
    }

    @Override
    public Seller save(Seller seller) {
        userRepository.save(UserJpaMapper.toEntity(seller));
        return seller;
    }

    @Override
    public Seller create(Seller seller) {
        jpaInserter.insert(UserJpaMapper.toEntity(seller));
        return seller;
    }

    @Override
    public Optional<Seller> findById(SellerId sellerId) {
        return userRepository.findById(sellerId.getValue())
                .filter(UserJpaMapper::isSeller)
                .map(UserJpaMapper::toSellerDomain);
    }

    @Override
    public List<Seller> findAll() {
        return userRepository.findByUserType(SELLER_TYPE).stream()
                .map(UserJpaMapper::toSellerDomain)
                .toList();
    }
}
