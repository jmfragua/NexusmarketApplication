package application.domain.services.user;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateUserActiveStatusService;
import application.domain.valuesObjects.EmailAddress;
import java.util.Objects;

/**
 * Identifies the user who is going to operate: every operation of the system is executed on behalf
 * of an authenticated user and only an active one can operate (RG-01, RD-ROL-01).
 *
 * <p>The business specification leaves technical authentication mechanisms explicitly out of the
 * domain (RD-ALC-01), so credential verification and token issuing belong to the security adapter.
 * This service resolves the user behind the email and guarantees they are allowed to operate.</p>
 */
public class LoginService {

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateUserActiveStatusService validateUserActiveStatusService;

    public LoginService(UserRepositoryPort userRepositoryPort,
                        ValidateUserActiveStatusService validateUserActiveStatusService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateUserActiveStatusService = Objects.requireNonNull(validateUserActiveStatusService,
                "the active status validation is mandatory");
    }

    /**
     * @param email main access channel of the user.
     * @return the authenticated user, guaranteed to be active.
     * @throws EntityNotFoundException when no user is registered under that email address.
     */
    public User<?> login(EmailAddress email) {
        Objects.requireNonNull(email, "email address is mandatory");
        User<?> user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("no user is registered under the email " + email));
        validateUserActiveStatusService.validate(user);
        return user;
    }
}
