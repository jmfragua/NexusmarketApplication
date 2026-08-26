package application.domain.valuesObjects;

/**
 * Identifier of a cart line. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class CartItemId extends Identifier {

    private CartItemId(String value) {
        super(value);
    }

    public static CartItemId of(String value) {
        return new CartItemId(value);
    }
}
