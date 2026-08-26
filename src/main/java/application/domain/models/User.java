package application.domain.models;

import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.Objects;
import lombok.Getter;

/**
 * Authentication and identification base of the marketplace. Represents every person authorized to
 * interact with the system.
 *
 * <p>Every operation of the system is executed on behalf of an authenticated user (RG-01) and no
 * participant may manage information outside their role (RG-03).</p>
 *
 * @param <ID> identifier specialization of the concrete kind of user.
 */
@Getter
public abstract class User<ID extends Identifier> extends DomainEntity<ID> {

    private FullName fullName;

    private EmailAddress email;

    private IdentityDocument identityDocument;

    private UserRole role;

    private UserStatus status;

    protected User(ID identifier,
                   FullName fullName,
                   EmailAddress email,
                   IdentityDocument identityDocument,
                   UserRole role,
                   UserStatus status) {
        super(identifier);
        this.fullName = Objects.requireNonNull(fullName, "full name is mandatory");
        this.email = Objects.requireNonNull(email, "email address is mandatory");
        this.identityDocument = Objects.requireNonNull(identityDocument, "identity document is mandatory");
        this.role = Objects.requireNonNull(role, "role is mandatory");
        this.status = Objects.requireNonNull(status, "status is mandatory");
    }

    /**
     * Blocks the user: from that moment they cannot execute operations.
     */
    public void block() {
        changeStatus(UserStatus.BLOCKED);
    }

    /**
     * Reactivates a blocked user.
     */
    public void activate() {
        changeStatus(UserStatus.ACTIVE);
    }

    /**
     * Reassigns the role. It stays unique per user (RG-02): the attribute holds a single value.
     */
    public void changeRole(UserRole newRole) {
        this.role = Objects.requireNonNull(newRole, "role is mandatory");
    }

    /**
     * Applies a new contact email. The format is guaranteed by {@link EmailAddress}; uniqueness
     * across the platform depends on the whole data set and is verified outside the value object
     * (RD-VO-09).
     */
    public void updateContactEmail(EmailAddress newEmail) {
        this.email = Objects.requireNonNull(newEmail, "email address is mandatory");
    }

    /**
     * Verifies that the resource belongs to the scope of the role of this user (RG-03).
     *
     * <p>Only an active user can operate (RG-01); ownership of the resource is decided by each
     * specialization.</p>
     */
    public final boolean canOperateOn(DomainEntity<?> resource) {
        return isActive() && resource != null && ownsResource(resource);
    }

    public boolean isActive() {
        return status.canOperate();
    }

    /**
     * Decides whether the resource falls inside the scope of this user.
     *
     * @param resource entity the user intends to operate on.
     * @return true when the resource belongs to the user.
     */
    protected abstract boolean ownsResource(DomainEntity<?> resource);

    private void changeStatus(UserStatus target) {
        if (this.status == target) {
            return;
        }
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException("user cannot move from " + this.status + " to " + target);
        }
        this.status = target;
    }
}
