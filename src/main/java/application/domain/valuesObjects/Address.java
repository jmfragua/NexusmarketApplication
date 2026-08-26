package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Delivery location of a buyer. Used as the mandatory primary address, as an optional additional
 * address, and as the delivery address carried by the order and the shipment.
 *
 * <p>The business specification describes the address as a "usual location for deliveries" without
 * breaking it down into street, city or country, so it is modelled as a single value and no field
 * outside the specification is added (RD-VO-17).</p>
 */
@Getter
public final class Address extends ValueObject {

    private final String value;

    private Address(String value) {
        this.value = requireNonBlank(value, "address");
    }

    public static Address of(String value) {
        return new Address(value);
    }

    @Override
    protected Object[] equalityComponents() {
        return new Object[] { value };
    }

    @Override
    public String toString() {
        return value;
    }
}
