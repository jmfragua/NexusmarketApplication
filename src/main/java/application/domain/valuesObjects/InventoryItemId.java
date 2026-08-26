package application.domain.valuesObjects;

/**
 * Identifier of a inventory item. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class InventoryItemId extends Identifier {

    private InventoryItemId(String value) {
        super(value);
    }

    public static InventoryItemId of(String value) {
        return new InventoryItemId(value);
    }
}
