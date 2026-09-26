package application.domain.exceptions;

import application.domain.valuesObjects.EmailAddress;

/**
 * Raised when an email address already registered on the platform is reused: the email is unique
 * across the whole platform (RD-ID-03, RD-VO-09).
 */
public class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(EmailAddress email) {
        super("the email address " + email + " is already registered on the platform");
    }
}
