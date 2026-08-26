package application.domain.valuesObjects;

/**
 * Identifier of a shipment. Distinct type from every other {@link Identifier} specialization, so
 * it cannot be used where another entity identifier is expected (RD-VO-03).
 */
public final class ShipmentId extends Identifier {

    private ShipmentId(String value) {
        super(value);
    }

    public static ShipmentId of(String value) {
        return new ShipmentId(value);
    }
}
