package application.domain.valuesObjects;

/**
 * Category of a logged business operation (RD-AUD-01).
 *
 * <p>A deliberately minimal, closed catalogue: only the operations the domain currently traces are
 * named here. Growing it is a conscious decision tied to a new {@code RegisterOperationLogService}
 * call site, never a free-text category (RD-VO-10, RD-VO-11).</p>
 */
public enum OperationType implements Enumeration {

    SELLER_REGISTERED("Seller registered. A seller was incorporated along with their first warehouse."),
    ORDER_STATUS_CHANGED("Order status changed. An order moved to the next state of its lifecycle."),
    INVENTORY_ADJUSTED("Inventory adjusted. The registered available stock of a product was corrected."),
    REFUND_ISSUED("Refund issued. A refund was applied over the invoice of a return.");

    private final String description;

    OperationType(String description) {
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
