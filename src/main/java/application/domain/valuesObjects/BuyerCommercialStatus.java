package application.domain.valuesObjects;

/**
 * Condition of a buyer to place purchases.
 *
 * <p>Lifecycle: {@code ENABLED <-> DISABLED}. Only an enabled buyer can confirm an order.</p>
 */
public enum BuyerCommercialStatus implements Enumeration {

    ENABLED("Enabled. The buyer can place purchases."),
    DISABLED("Disabled. The buyer cannot place purchases.");

    private final String description;

    BuyerCommercialStatus(String description) {
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
    public boolean canTransitionTo(BuyerCommercialStatus target) {
        return target != null && target != this;
    }

    public boolean allowsPurchase() {
        return this == ENABLED;
    }
}
