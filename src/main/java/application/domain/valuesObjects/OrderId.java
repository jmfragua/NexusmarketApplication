package application.domain.valuesObjects;

/**
 * Identifier of a order. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class OrderId extends Identifier {

    private OrderId(String value) {
        super(value);
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }
}
