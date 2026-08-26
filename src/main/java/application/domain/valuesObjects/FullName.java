package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Official name of a user. Mandatory and never blank.
 */
@Getter
public final class FullName extends ValueObject {

    private final String value;

    private FullName(String value) {
        this.value = requireNonBlank(value, "full name");
    }

    public static FullName of(String value) {
        return new FullName(value);
    }

    @Override
    protected Object[] equalityComponents() {
        return new Object[] { value };
    }

    @Override
    public String toString() {
        return value;
    }
}
