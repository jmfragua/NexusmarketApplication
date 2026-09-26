package application.domain.services.user;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Blocks a user: from that moment they can no longer execute operations (RG-01).
 *
 * <p>The operational status of the users is managed by the administrator (RD-ROL-06).</p>
 */
public class BlockUserService {

    private static final String OPERATION = "block user";

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public BlockUserService(UserRepositoryPort userRepositoryPort,
                            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param actor  administrator performing the change.
     * @param target user being blocked.
     * @return the blocked user.
     */
    public User<?> block(User<?> actor, User<?> target) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(target, "the user to block is mandatory");
        target.block();
        return userRepositoryPort.save(target);
    }
}
