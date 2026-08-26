package application.domain.valuesObjects;

/**
 * Identifier of a user. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class UserId extends Identifier {

    private UserId(String value) {
        super(value);
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
