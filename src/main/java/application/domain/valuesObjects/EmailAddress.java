package application.domain.valuesObjects;

import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Main access and communication channel of a user. Mandatory and well formed.
 *
 * <p>Uniqueness across the platform depends on the whole data set and therefore cannot be checked
 * inside the value object (RD-VO-09): this type guarantees the format, uniqueness is verified when
 * the user joins the domain.</p>
 */
@Getter
public final class EmailAddress extends ValueObject {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final String value;

    private EmailAddress(String value) {
        String candidate = requireNonBlank(value, "email address").toLowerCase();
        if (!FORMAT.matcher(candidate).matches()) {
            throw new IllegalArgumentException("email address is not well formed: " + candidate);
        }
        this.value = candidate;
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
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
