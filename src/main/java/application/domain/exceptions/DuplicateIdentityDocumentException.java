package application.domain.exceptions;

import application.domain.valuesObjects.IdentityDocument;

/**
 * Raised when an identity document already registered on the platform is reused: the document is
 * unique across the whole platform (RD-ID-04, RD-VO-09).
 */
public class DuplicateIdentityDocumentException extends DomainException {

    public DuplicateIdentityDocumentException(IdentityDocument identityDocument) {
        super("the identity document " + identityDocument + " is already registered on the platform");
    }
}
