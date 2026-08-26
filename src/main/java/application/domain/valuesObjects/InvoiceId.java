package application.domain.valuesObjects;

/**
 * Identifier of a invoice. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class InvoiceId extends Identifier {

    private InvoiceId(String value) {
        super(value);
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(value);
    }
}
