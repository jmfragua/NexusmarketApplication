package application.domain.exceptions;

import application.domain.valuesObjects.UserRole;

/**
 * Raised when the administrator asks to incorporate staff with a role this operation does not create:
 * only logistics operators and supervisors are incorporated as staff (RD-ROL-09). Buyers register
 * themselves, sellers are incorporated with their first warehouse (RD-ROL-05), and administrators
 * are seeded or reassigned among staff.
 */
public class StaffRoleNotAllowedException extends DomainException {

    public StaffRoleNotAllowedException(UserRole role) {
        super("role " + role + " cannot be incorporated as staff; only LOGISTICS_OPERATOR and SUPERVISOR can");
    }
}
