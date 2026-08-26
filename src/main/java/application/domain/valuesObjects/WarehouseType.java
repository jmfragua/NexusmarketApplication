package application.domain.valuesObjects;

/**
 * Classification of the warehouses by ownership.
 *
 * <p>The type determines the specialization of the warehouse entity and never changes during its
 * life (RD-VO-14).</p>
 */
public enum WarehouseType implements Enumeration {

    MARKETPLACE("Marketplace warehouse, operated by the platform."),
    SELLER("Seller warehouse, tied to its owner.");

    private final String description;

    WarehouseType(String description) {
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
}
