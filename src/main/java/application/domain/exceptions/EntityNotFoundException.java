package application.domain.exceptions;

import application.domain.valuesObjects.Identifier;

/**
 * Raised when an operation references an entity that does not exist in the domain.
 */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, Identifier identifier) {
        super(entityName + " " + identifier + " does not exist");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
