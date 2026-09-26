package application.domain.services.user;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Reactivates a blocked user, who can execute operations again (RG-01).
 */
public class ActivateUserService {

    private static final String OPERATION = "activate user";

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ActivateUserService(UserRepositoryPort userRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param actor  administrator performing the change.
     * @param target user being reactivated.
     * @return the active user.
     */
    public User<?> activate(User<?> actor, User<?> target) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(target, "the user to activate is mandatory");
        target.activate();
        return userRepositoryPort.save(target);
    }
}
