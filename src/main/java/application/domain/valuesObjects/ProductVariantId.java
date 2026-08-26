package application.domain.valuesObjects;

/**
 * Identifier of a product variant. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class ProductVariantId extends Identifier {

    private ProductVariantId(String value) {
        super(value);
    }

    public static ProductVariantId of(String value) {
        return new ProductVariantId(value);
    }
}
