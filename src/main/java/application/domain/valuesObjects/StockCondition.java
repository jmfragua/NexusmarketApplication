package application.domain.valuesObjects;

/**
 * Condition of the stock held in a warehouse.
 *
 * <p>Lifecycle: {@code AVAILABLE -> DAMAGED}. Stock marked as damaged is excluded from every
 * reservation (RD-INV-03).</p>
 */
public enum StockCondition implements Enumeration {

    AVAILABLE("Available. The stock can be reserved and sold."),
    DAMAGED("Damaged. The stock is excluded from every reservation.");

    private final String description;

    StockCondition(String description) {
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
     * Declares the allowed transitions; every transition not declared here is forbidden
     * (RD-VO-12).
     */
    public boolean canTransitionTo(StockCondition target) {
        return this == AVAILABLE && target == DAMAGED;
    }

    public boolean allowsReservation() {
        return this == AVAILABLE;
    }
}
