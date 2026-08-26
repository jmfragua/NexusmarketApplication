package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Unique identifier of an entity within its own type (RD-ID-01, RD-VO-02).
 *
 * <p>Every entity type owns its specialization, so identifiers of different entities are not
 * interchangeable: a {@code OrderId} cannot be used where a {@code ProductId} is expected even
 * though both wrap a string (RD-VO-03).</p>
 */
@Getter
public abstract class Identifier extends ValueObject {

    private final String value;

    protected Identifier(String value) {
        this.value = requireNonBlank(value, "identifier value");
    }

    @Override
    protected Object[] equalityComponents() {
        return new Object[] { value };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + value + ")";
    }
}
