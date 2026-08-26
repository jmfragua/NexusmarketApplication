package application.domain.valuesObjects;

import java.util.Arrays;

/**
 * Abstract root of every value object of the domain (RD-VO-01, RD-VO-04, RD-VO-07).
 *
 * <p>A value object has no identity of its own: two instances holding the same attributes are
 * indistinguishable and interchangeable. Instances are immutable, are fully validated on
 * construction and any "modification" returns a brand new instance.</p>
 */
public abstract class ValueObject {

    /**
     * Attributes that take part in the equality of the value object.
     *
     * @return every attribute that defines the value, in a stable order.
     */
    protected abstract Object[] equalityComponents();

    /**
     * Compares by the whole set of attributes and by concrete type, so that two specializations
     * wrapping the same raw value are never interchangeable (RD-VO-03).
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(equalityComponents(), ((ValueObject) other).equalityComponents());
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(equalityComponents());
    }

    /**
     * Guard shared by every value object built on top of a mandatory, non blank text (RD-VO-08).
     *
     * @param value         raw text to validate.
     * @param attributeName attribute name reported when the value is rejected.
     * @return the trimmed text, guaranteed to be present.
     */
    protected static String requireNonBlank(String value, String attributeName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(attributeName + " is mandatory and must not be blank");
        }
        return value.trim();
    }
}
