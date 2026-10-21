package application.domain.exceptions;

import application.domain.valuesObjects.Identifier;

/**
 * Raised when a new entity is created with an identifier another entity of the same kind already
 * holds. Identifiers are supplied by the caller (RD-POUT-04), so a creation is refused instead of
 * replacing what exists: a creation never acts as an update (RD-ID-01, RD-ID-06).
 *
 * <p>Users and stock records keep their own, more specific exceptions
 * ({@link DuplicateUserIdentifierException}, {@link DuplicateInventoryItemException}).</p>
 */
public class DuplicateIdentifierException extends DomainException {

    public DuplicateIdentifierException(String entity, Identifier identifier) {
        super("identifier " + identifier + " is already taken by an existing " + entity);
    }
}
