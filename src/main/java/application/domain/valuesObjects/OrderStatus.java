package application.domain.valuesObjects;

/**
 * State of an order along its lifecycle. Central catalogue of the system.
 *
 * <p>Lifecycle: {@code CART -> PENDING_PAYMENT -> PAID -> DISPATCHED -> DELIVERED}. The cycle is
 * strictly sequential: no jumps and no steps backwards are admitted. {@code DELIVERED} is terminal
 * and a finished order can never be modified again (RD-PED-01, RD-PED-02, RD-VO-13).</p>
 */
public enum OrderStatus implements Enumeration {

    CART("Cart. Provisional selection of products."),
    PENDING_PAYMENT("Pending payment. Waiting for the financial confirmation."),
    PAID("Paid. Preparation processes start."),
    DISPATCHED("Dispatched. Physical exit from the warehouse."),
    DELIVERED("Delivered. Successful completion of the delivery.");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String code() {
        return name();
    }

    /**
     * The only allowed transition is the immediate next state of the sequence (RD-PED-01).
     */
    public boolean canTransitionTo(OrderStatus target) {
        return target != null && target.ordinal() == this.ordinal() + 1;
    }

    /**
     * @return true for the terminal state, which admits no outgoing transition (RD-VO-13).
     */
    public boolean isTerminal() {
        return this == DELIVERED;
    }

    /**
     * A finished order cannot be modified under any circumstance (RD-PED-02).
     */
    public boolean isModifiable() {
        return !isTerminal();
    }
}
