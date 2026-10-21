package application.domain.services.user;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.DuplicateUserIdentifierException;
import application.domain.exceptions.StaffRoleNotAllowedException;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.Objects;
import java.util.Set;

/**
 * Incorporates a staff user — a logistics operator or a supervisor — on behalf of an administrator.
 *
 * <p>The administrator manages the information of every user of the platform (OBJ-01), and without
 * this operation the logistics and supervision roles could not exist at all: buyers register
 * themselves and sellers are incorporated with their first warehouse, so nothing else creates a
 * {@link StaffUser} (RD-ROL-07, RD-ROL-09).</p>
 */
public class RegisterStaffUserService {

    private static final String OPERATION = "incorporate staff user";

    private static final Set<UserRole> INCORPORABLE_ROLES = Set.of(UserRole.LOGISTICS_OPERATOR, UserRole.SUPERVISOR);

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public RegisterStaffUserService(UserRepositoryPort userRepositoryPort,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @return the new staff user, active and holding exactly the requested role (RG-02).
     * @throws StaffRoleNotAllowedException        when the role is not logistics operator or supervisor.
     * @throws DuplicateUserIdentifierException    when another user already holds the identifier (RD-ID-02).
     * @throws DuplicateEmailException             when the email address is already registered (RD-ID-03).
     * @throws DuplicateIdentityDocumentException  when the identity document is already registered (RD-ID-04).
     */
    public StaffUser register(User<?> actor,
                              UserId userId,
                              FullName fullName,
                              EmailAddress email,
                              IdentityDocument identityDocument,
                              UserRole role) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        Objects.requireNonNull(role, "the role is mandatory");
        if (!INCORPORABLE_ROLES.contains(role)) {
            throw new StaffRoleNotAllowedException(role);
        }
        if (userRepositoryPort.findByIdentifier(userId).isPresent()) {
            throw new DuplicateUserIdentifierException(userId);
        }
        if (userRepositoryPort.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        if (userRepositoryPort.existsByIdentityDocument(identityDocument)) {
            throw new DuplicateIdentityDocumentException(identityDocument);
        }
        StaffUser staffUser = StaffUser.of(userId, fullName, email, identityDocument, role, UserStatus.ACTIVE);
        return userRepositoryPort.save(staffUser);
    }
}
