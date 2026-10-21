package application.domain.exceptions;

import application.domain.valuesObjects.Identifier;

/**
 * Raised when a new user is registered with an identifier that another user already holds: the
 * identifier of a user is unique (RD-ID-02). Identifiers are supplied by the caller (RD-POUT-04), so
 * without this check saving the new user would silently replace the existing one.
 */
public class DuplicateUserIdentifierException extends DomainException {

    public DuplicateUserIdentifierException(Identifier identifier) {
        super("a user with identifier " + identifier + " already exists");
    }
}
