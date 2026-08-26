package application.domain.valuesObjects;

/**
 * Identifier of a inventory movement. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class InventoryMovementId extends Identifier {

    private InventoryMovementId(String value) {
        super(value);
    }

    public static InventoryMovementId of(String value) {
        return new InventoryMovementId(value);
    }
}
