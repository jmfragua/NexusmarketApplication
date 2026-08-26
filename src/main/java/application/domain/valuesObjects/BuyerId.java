package application.domain.valuesObjects;

/**
 * Identifier of a buyer. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class BuyerId extends Identifier {

    private BuyerId(String value) {
        super(value);
    }

    public static BuyerId of(String value) {
        return new BuyerId(value);
    }
}
