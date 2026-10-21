package application.domain.support;

import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory double of {@link UserRepositoryPort}.
 *
 * <p>Hand written rather than mocked: the tests exercise real business rules, and a fake that
 * actually stores what it is given catches mistakes a stubbed call never would — such as a service
 * that validates uniqueness but forgets to persist.</p>
 */
public class InMemoryUserRepository implements UserRepositoryPort {

    private final Map<String, User<?>> users = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <U extends User<?>> U save(U user) {
        users.put(user.getIdentifier().getValue(), user);
        return (U) users.get(user.getIdentifier().getValue());
    }

    @Override
    public <U extends User<?>> U create(U user) {
        if (users.containsKey(user.getIdentifier().getValue())) {
            throw new IllegalStateException("identifier already taken: " + user.getIdentifier());
        }
        return save(user);
    }

    @Override
    public Optional<User<?>> findByIdentifier(Identifier identifier) {
        return Optional.ofNullable(users.get(identifier.getValue()));
    }

    @Override
    public Optional<Buyer> findBuyerById(BuyerId buyerId) {
        return findByIdentifier(buyerId)
                .filter(Buyer.class::isInstance)
                .map(Buyer.class::cast);
    }

    @Override
    public Optional<User<?>> findByEmail(EmailAddress email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean existsByIdentityDocument(IdentityDocument identityDocument) {
        return users.values().stream()
                .anyMatch(user -> user.getIdentityDocument().equals(identityDocument));
    }

    @Override
    public List<User<?>> findByRole(UserRole role) {
        return users.values().stream()
                .filter(user -> user.getRole() == role)
                .toList();
    }

    @Override
    public List<User<?>> findAll() {
        return new ArrayList<>(users.values());
    }
}
