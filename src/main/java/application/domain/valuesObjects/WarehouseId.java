package application.domain.valuesObjects;

/**
 * Identifier of a warehouse. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class WarehouseId extends Identifier {

    private WarehouseId(String value) {
        super(value);
    }

    public static WarehouseId of(String value) {
        return new WarehouseId(value);
    }
}
