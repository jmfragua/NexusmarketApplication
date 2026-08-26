package application.domain.valuesObjects;

/**
 * Identifier of a refund. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class RefundId extends Identifier {

    private RefundId(String value) {
        super(value);
    }

    public static RefundId of(String value) {
        return new RefundId(value);
    }
}
