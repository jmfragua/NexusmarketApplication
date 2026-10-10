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

    /**
     * Whether the role owns its own entity instead of living in {@code StaffUser} (RD-ROL-07).
     *
     * <p>{@code BUYER} and {@code SELLER} do: they carry state and creation rules of their own. The
     * other three are nothing but a value of this attribute, which is what makes reassignment among
     * them possible (RD-ROL-08).</p>
     */
    public boolean ownsSpecialization() {
        return this == BUYER || this == SELLER;
    }

    /**
     * Whether a user holding this role may be reassigned to {@code newRole} by changing the
     * attribute alone (RD-ROL-08).
     */
    public boolean canBeReassignedTo(UserRole newRole) {
        if (this == newRole) {
            return true;
        }
        return !ownsSpecialization() && !newRole.ownsSpecialization();
    }
}
