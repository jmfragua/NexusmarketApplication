package application.domain.exceptions;

import application.domain.valuesObjects.UserRole;

/**
 * Raised when a role reassignment would cross the boundary of a specialization.
 *
 * <p>{@code BUYER} and {@code SELLER} own their own entity ({@code Buyer}, {@code Seller}) with
 * state and creation rules the other roles do not have: a buyer carries addresses, a commercial
 * status and a cart, and a seller only comes into existence incorporated by an administrator along
 * with their first warehouse (RD-ROL-05). Reassigning the role attribute cannot turn one into the
 * other — it would leave that state orphaned, or conjure a seller without the incorporation the
 * rule demands.</p>
 *
 * <p>The three roles with no specialization of their own — logistics operator, administrator and
 * supervisor — all live in {@code StaffUser} (RD-ROL-07), so moving between them is a genuine
 * attribute change and remains allowed (RD-ROL-02, RD-ROL-08).</p>
 */
public class InvalidRoleChangeException extends DomainException {

    public InvalidRoleChangeException(UserRole currentRole, UserRole newRole) {
        super("role " + currentRole + " cannot be reassigned to " + newRole
                + ": one of them owns its own specialization and must be created through it");
    }
}
