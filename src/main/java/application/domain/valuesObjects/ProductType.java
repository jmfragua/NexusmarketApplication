package application.domain.valuesObjects;

/**
 * Nature of a product in the catalogue. Drives the operational behaviour of the product across the
 * whole domain.
 *
 * <p>The type is set when the product is registered and never changes during its life (RD-CAT-02,
 * RD-VO-14).</p>
 */
public enum ProductType implements Enumeration {

    PHYSICAL("Physical product. Requires inventory and dispatch."),
    DIGITAL("Digital product. Immediate delivery once the payment is confirmed.");

    private final String description;

    ProductType(String description) {
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
     * Digital products never generate stock nor inventory movements (RD-INV-05).
     */
    public boolean requiresInventory() {
        return this == PHYSICAL;
    }
}
