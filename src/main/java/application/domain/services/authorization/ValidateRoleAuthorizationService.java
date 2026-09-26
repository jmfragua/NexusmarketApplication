package application.domain.services.authorization;

import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.User;
import application.domain.valuesObjects.UserRole;
import java.util.Arrays;
import java.util.Objects;

/**
 * Verifies that the role of the actor covers the requested operation.
 *
 * <p>Every user plays exactly one role (RG-02) and no participant manages information outside their
 * role (RG-03, RD-ROL-03). The responsibility matrix of RD-ROL-06 decides which role executes which
 * process.</p>
 */
public class ValidateRoleAuthorizationService {

    private final ValidateUserActiveStatusService validateUserActiveStatusService;

    public ValidateRoleAuthorizationService(ValidateUserActiveStatusService validateUserActiveStatusService) {
        this.validateUserActiveStatusService = Objects.requireNonNull(validateUserActiveStatusService,
                "the active status validation is mandatory");
    }

    /**
     * @param operation    name of the process being executed, reported when the role is rejected.
     * @param allowedRoles roles the responsibility matrix authorizes for that process (RD-ROL-06).
     * @throws UnauthorizedOperationException when the role of the actor is not among the allowed
     *                                        ones.
     */
    public void validate(User<?> actor, String operation, UserRole... allowedRoles) {
        validateUserActiveStatusService.validate(actor);
        boolean allowed = Arrays.asList(allowedRoles).contains(actor.getRole());
        if (!allowed) {
            throw new UnauthorizedOperationException(actor.getRole(), operation);
        }
    }

    /**
     * The supervisor profile only consults: it never executes modification processes (RD-ROL-06).
     */
    public void validateCanModify(User<?> actor, String operation) {
        validateUserActiveStatusService.validate(actor);
        if (!actor.getRole().canModifyBusinessData()) {
            throw new UnauthorizedOperationException(actor.getRole(), operation);
        }
    }
}
