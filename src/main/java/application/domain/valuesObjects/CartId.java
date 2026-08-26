package application.domain.valuesObjects;

/**
 * Identifier of a cart. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class CartId extends Identifier {

    private CartId(String value) {
        super(value);
    }

    public static CartId of(String value) {
        return new CartId(value);
    }
}
