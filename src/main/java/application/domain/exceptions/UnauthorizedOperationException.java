package application.domain.exceptions;

import application.domain.valuesObjects.UserRole;

/**
 * Raised when a user attempts an operation outside the scope of their role (RG-03, RD-ROL-03).
 */
public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(UserRole role, String operation) {
        super("role " + role + " is not allowed to execute the operation " + operation);
    }

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
