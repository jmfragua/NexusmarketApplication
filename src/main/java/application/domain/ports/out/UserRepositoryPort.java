package application.domain.ports.out;

import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the users of the platform.
 *
 * <p>The uniqueness of the email address and the identity document depends on the whole data set
 * and therefore cannot be checked inside a value object (RD-VO-09): this port exposes the queries
 * the domain services use to enforce RD-ID-03 and RD-ID-04.</p>
 */
public interface UserRepositoryPort {

    <U extends User<?>> U save(U user);

    Optional<User<?>> findByIdentifier(Identifier identifier);

    Optional<Buyer> findBuyerById(BuyerId buyerId);

    Optional<User<?>> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    boolean existsByIdentityDocument(IdentityDocument identityDocument);

    List<User<?>> findByRole(UserRole role);

    List<User<?>> findAll();
}
