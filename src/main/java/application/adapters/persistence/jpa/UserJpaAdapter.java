package application.adapters.persistence.jpa;

import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.repositories.SpringDataJpaUserRepository;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Relational adapter of {@link UserRepositoryPort}.
 */
@Repository
public class UserJpaAdapter implements UserRepositoryPort {

    private final SpringDataJpaUserRepository userRepository;

    public UserJpaAdapter(SpringDataJpaUserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "the user repository is mandatory");
    }

    /**
     * Returns the very same aggregate it received: it already carries its identity, and rebuilding
     * it from the stored row would needlessly lose the concrete subtype the caller handed over.
     */
    @Override
    public <U extends User<?>> U save(U user) {
        userRepository.save(UserJpaMapper.toEntity(user));
        return user;
    }

    @Override
    public Optional<User<?>> findByIdentifier(Identifier identifier) {
        return userRepository.findById(identifier.getValue()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<Buyer> findBuyerById(BuyerId buyerId) {
        return userRepository.findById(buyerId.getValue())
                .filter(UserJpaMapper::isBuyer)
                .map(UserJpaMapper::toBuyerDomain);
    }

    @Override
    public Optional<User<?>> findByEmail(EmailAddress email) {
        return userRepository.findByEmail(email.getValue()).map(UserJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return userRepository.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByIdentityDocument(IdentityDocument identityDocument) {
        return userRepository.existsByIdentityDocument(identityDocument.getValue());
    }

    @Override
    public List<User<?>> findByRole(UserRole role) {
        return userRepository.findByRole(role.code()).stream()
                .<User<?>>map(UserJpaMapper::toDomain)
                .toList();
    }

    @Override
    public List<User<?>> findAll() {
        return userRepository.findAll().stream()
                .<User<?>>map(UserJpaMapper::toDomain)
                .toList();
    }
}
