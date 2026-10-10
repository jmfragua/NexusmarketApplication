package application.domain.models;

import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.Objects;

/**
 * User of a role that owns no specialization of its own: logistics operator, administrator or
 * supervisor.
 *
 * <p>The specification states that of the five participants only buyer and seller receive their own
 * attributes and rules; the other three are represented through the {@code UserRole} value object
 * over {@code User}. Since {@link User} is abstract, something concrete still has to carry them —
 * that is this class, which adds no attribute and exists only so those three roles can exist as
 * objects at all (RD-ROL-07).</p>
 *
 * <p>Its scope is restricted on purpose: it refuses the roles that do have a specialization, so
 * nobody can build a buyer or a seller that bypasses their own rules.</p>
 */
public class StaffUser extends User<UserId> {

    private StaffUser(UserId identifier,
                      FullName fullName,
                      EmailAddress email,
                      IdentityDocument identityDocument,
                      UserRole role,
                      UserStatus status) {
        super(identifier, fullName, email, identityDocument, role, status);
    }

    /**
     * @param role one of {@code LOGISTICS_OPERATOR}, {@code ADMINISTRATOR} or {@code SUPERVISOR}.
     * @throws IllegalArgumentException when the role has a specialization of its own, which must be
     *                                  built through it ({@code Buyer}, {@code Seller}).
     */
    public static StaffUser of(UserId identifier,
                               FullName fullName,
                               EmailAddress email,
                               IdentityDocument identityDocument,
                               UserRole role,
                               UserStatus status) {
        Objects.requireNonNull(role, "role is mandatory");
        if (role == UserRole.BUYER || role == UserRole.SELLER) {
            throw new IllegalArgumentException(
                    "role " + role + " owns its own specialization and must be built through it");
        }
        return new StaffUser(identifier, fullName, email, identityDocument, role, status);
    }

    /**
     * These roles own no resource of their own: a warehouse belongs to a seller, an order to a
     * buyer. Their reach is decided by their role through
     * {@code ValidateRoleAuthorizationService}, not by ownership (RD-ROL-06).
     */
    @Override
    protected boolean ownsResource(DomainEntity<?> resource) {
        return false;
    }
}
