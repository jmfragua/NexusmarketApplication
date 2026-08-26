package application.domain.valuesObjects;

/**
 * Type of movement affecting the stock. Closed set of the five movements defined by the business.
 *
 * <p>No stock change can happen without a movement of one of these types, and none of them may
 * leave the balance below zero (RD-INV-04, RD-INV-02).</p>
 */
public enum InventoryMovementType implements Enumeration {

    INBOUND("Inbound. Stock registered into the warehouse."),
    RESERVATION("Reservation. Stock committed to an order."),
    SALE_OUTBOUND("Sale outbound. Physical exit of stock for a dispatched order."),
    ADJUSTMENT("Adjustment. Correction of the registered stock."),
    RETURN("Return. Stock reintegrated from a product return.");

    private final String description;

    InventoryMovementType(String description) {
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
     * @return true for the movements that increase the available stock.
     */
    public boolean isInbound() {
        return this == INBOUND || this == RETURN;
    }

    /**
     * @return true for the movement that takes stock physically out of the warehouse.
     */
    public boolean isOutbound() {
        return this == SALE_OUTBOUND;
    }
}
