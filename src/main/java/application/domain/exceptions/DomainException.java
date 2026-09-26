package application.domain.exceptions;

/**
 * Root of every exception raised when a business rule of the domain is violated.
 *
 * <p>The domain never signals a rule violation with a generic exception: every rule owns a
 * specialization of this type, so the offended rule stays explicit and traceable (RD-ALC-03).</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
