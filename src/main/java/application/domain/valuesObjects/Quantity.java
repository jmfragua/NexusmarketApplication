package application.domain.valuesObjects;

import lombok.Getter;

/**
 * Number of units handled by the domain: available stock, reserved stock, quantity of an inventory
 * movement, quantity selected in a cart line or requested in an order line.
 *
 * <p>Negative stock is never allowed under any circumstance (RD-INV-02), so the type itself makes a
 * negative quantity impossible to build, and {@link #subtract(Quantity)} rejects any operation that
 * would end below zero.</p>
 */
@Getter
public final class Quantity extends ValueObject {

    private static final Quantity ZERO = new Quantity(0);

    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must be greater than or equal to zero, got " + value);
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return value == 0 ? ZERO : new Quantity(value);
    }

    public static Quantity zero() {
        return ZERO;
    }

    public Quantity add(Quantity other) {
        return of(this.value + requireQuantity(other).value);
    }

    /**
     * @throws IllegalArgumentException if the result would be negative (RD-INV-02).
     */
    public Quantity subtract(Quantity other) {
        int result = this.value - requireQuantity(other).value;
        if (result < 0) {
            throw new IllegalArgumentException("quantity cannot become negative: " + this.value + " - " + other.value);
        }
        return of(result);
    }

    /**
     * Supports the reservation validation: there is enough stock to cover the requested amount.
     */
    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value >= requireQuantity(other).value;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    /**
     * A movement, a cart line or an order line can never be of zero units.
     */
    public boolean isPositive() {
        return this.value > 0;
    }

    private static Quantity requireQuantity(Quantity other) {
        if (other == null) {
            throw new IllegalArgumentException("quantity operand is mandatory");
        }
        return other;
    }

    @Override
    protected Object[] equalityComponents() {
        return new Object[] { value };
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
