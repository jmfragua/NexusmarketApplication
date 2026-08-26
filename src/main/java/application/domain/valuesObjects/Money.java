package application.domain.valuesObjects;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Commercial value tied to a sale: amount of an order line, order total, invoiced total and
 * refunded amount.
 *
 * <p>A commercial value is never negative and is always expressed in a currency. Only amounts of
 * the same currency can be added or compared.</p>
 */
@Getter
public final class Money extends ValueObject {

    private final BigDecimal amount;

    private final String currency;

    private Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("monetary amount is mandatory");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("monetary amount cannot be negative, got " + amount);
        }
        this.amount = amount;
        this.currency = requireNonBlank(currency, "currency").toUpperCase();
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(requireSameCurrency(other).amount), this.currency);
    }

    /**
     * @throws IllegalArgumentException if the result would be negative.
     */
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(requireSameCurrency(other).amount), this.currency);
    }

    public Money multiply(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity is mandatory to multiply a monetary amount");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity.getValue())), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(requireSameCurrency(other).amount) > 0;
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    /**
     * @return true when both amounts share the currency, whatever the scale they were built with.
     */
    public boolean hasSameCurrencyAs(Money other) {
        return other != null && this.currency.equals(other.currency);
    }

    private Money requireSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("monetary operand is mandatory");
        }
        if (!hasSameCurrencyAs(other)) {
            throw new IllegalArgumentException(
                    "only amounts of the same currency can be operated: " + this.currency + " and " + other.currency);
        }
        return other;
    }

    /**
     * The scale is irrelevant to the commercial value, so {@code 10} and {@code 10.00} are the same
     * amount.
     */
    @Override
    protected Object[] equalityComponents() {
        return new Object[] { amount.stripTrailingZeros(), currency };
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
