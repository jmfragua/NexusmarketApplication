package application.domain.valuesObjects;

/**
 * Identifier of a order line. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class OrderLineId extends Identifier {

    private OrderLineId(String value) {
        super(value);
    }

    public static OrderLineId of(String value) {
        return new OrderLineId(value);
    }
}
