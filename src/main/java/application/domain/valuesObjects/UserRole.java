package application.domain.valuesObjects;

/**
 * Responsibilities and permissions of a user (RG-02, RG-03).
 *
 * <p>Every participant plays exactly one role and can only interact with the information that
 * belongs to their duties.</p>
 */
public enum UserRole implements Enumeration {

    BUYER("Buyer. Acquires published products."),
    SELLER("Seller. Registers and manages their own products."),
    LOGISTICS_OPERATOR("Logistics operator. Runs the physical operation of warehouses and dispatches."),
    ADMINISTRATOR("Administrator. Manages sellers and warehouses."),
    SUPERVISOR("Supervisor. Read only monitoring profile.");

    private final String description;

    UserRole(String description) {
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
     * The supervisor profile only consults: it never executes modification processes (RD-ROL-06).
     */
    public boolean canModifyBusinessData() {
        return this != SUPERVISOR;
    }
}
