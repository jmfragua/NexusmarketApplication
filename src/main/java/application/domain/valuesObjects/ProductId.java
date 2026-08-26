package application.domain.valuesObjects;

/**
 * Identifier of a product. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class ProductId extends Identifier {

    private ProductId(String value) {
        super(value);
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }
}
