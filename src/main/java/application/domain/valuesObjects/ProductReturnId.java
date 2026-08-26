package application.domain.valuesObjects;

/**
 * Identifier of a product return. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class ProductReturnId extends Identifier {

    private ProductReturnId(String value) {
        super(value);
    }

    public static ProductReturnId of(String value) {
        return new ProductReturnId(value);
    }
}
