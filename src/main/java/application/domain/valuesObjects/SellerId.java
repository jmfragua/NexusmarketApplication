package application.domain.valuesObjects;

/**
 * Identifier of a seller. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class SellerId extends Identifier {

    private SellerId(String value) {
        super(value);
    }

    public static SellerId of(String value) {
        return new SellerId(value);
    }
}
