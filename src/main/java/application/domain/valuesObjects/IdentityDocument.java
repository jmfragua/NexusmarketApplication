package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Identity document of a user. Never blank.
 *
 * <p>As with {@link EmailAddress}, uniqueness across the platform is a data set restriction and is
 * verified when the user joins the domain, not here (RD-VO-09).</p>
 */
@Getter
public final class IdentityDocument extends ValueObject {

    private final String value;

    private IdentityDocument(String value) {
        this.value = requireNonBlank(value, "identity document");
    }

    public static IdentityDocument of(String value) {
        return new IdentityDocument(value);
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
