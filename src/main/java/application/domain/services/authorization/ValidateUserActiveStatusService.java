package application.domain.services.authorization;

import application.domain.exceptions.UserNotActiveException;
import application.domain.models.User;
import java.util.Objects;

/**
 * Verifies that the actor of an operation is active on the platform.
 *
 * <p>Every operation of the system is executed on behalf of an authenticated user and only an
 * active user can execute operations (RG-01, RD-ROL-01).</p>
 */
public class ValidateUserActiveStatusService {

    /**
     * @throws UserNotActiveException when the user is blocked.
     */
    public void validate(User<?> actor) {
        Objects.requireNonNull(actor, "the actor of the operation is mandatory");
        if (!actor.isActive()) {
            throw new UserNotActiveException(actor.getIdentifier());
        }
    }
}
