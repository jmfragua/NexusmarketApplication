package application.domain.models;

import application.domain.valuesObjects.Identifier;
import java.util.Objects;
import lombok.Getter;

/**
 * Abstract base of every domain object owning an identity and a lifecycle (RD-ID-01).
 *
 * <p>Two entities are the same one when they share the identifier, even if the rest of their
 * attributes differ. Comparison is therefore made by identity, never by the value of the
 * attributes.</p>
 *
 * @param <ID> specialization of {@link Identifier} that identifies this kind of entity, so that
 *             identifiers of different entities are never interchangeable (RD-VO-03).
 */
@Getter
public abstract class DomainEntity<ID extends Identifier> {

    private final ID identifier;

    protected DomainEntity(ID identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier is mandatory");
    }

    /**
     * Compares entities by identity, never by attributes.
     */
    public boolean sameIdentityAs(DomainEntity<?> other) {
        return other != null && getClass() == other.getClass() && identifier.equals(other.identifier);
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof DomainEntity<?> entity && sameIdentityAs(entity);
    }

    @Override
    public final int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + identifier.getValue() + "]";
    }
}
