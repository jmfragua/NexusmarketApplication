package application.domain.exceptions;

import application.domain.valuesObjects.Identifier;

/**
 * Raised when a blocked user attempts to execute an operation: only an active user can operate
 * (RG-01, RD-ROL-01).
 */
public class UserNotActiveException extends DomainException {

    public UserNotActiveException(Identifier identifier) {
        super("user " + identifier + " is blocked and cannot execute operations");
    }
}
