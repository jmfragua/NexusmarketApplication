package application.domain.services.user;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Reassigns the role of a user. The role stays unique per user: the attribute admits a single value
 * (RG-02, RD-ROL-02).
 */
public class ChangeUserRoleService {

    private static final String OPERATION = "change user role";

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ChangeUserRoleService(UserRepositoryPort userRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param actor   administrator performing the reassignment.
     * @param target  user receiving the new role.
     * @param newRole role assigned to the user.
     * @return the user with the reassigned role.
     */
    public User<?> changeRole(User<?> actor, User<?> target, UserRole newRole) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(target, "the user whose role changes is mandatory");
        target.changeRole(newRole);
        return userRepositoryPort.save(target);
    }
}
