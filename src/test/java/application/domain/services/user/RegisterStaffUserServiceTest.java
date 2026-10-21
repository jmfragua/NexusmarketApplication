package application.domain.services.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.DuplicateUserIdentifierException;
import application.domain.exceptions.StaffRoleNotAllowedException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.StaffUser;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The administrator incorporates logistics operators and supervisors (RD-ROL-09), with the same
 * uniqueness rules as every other user (RD-ID-02, RD-ID-03, RD-ID-04).
 */
class RegisterStaffUserServiceTest {

    private ServiceHarness harness;

    private RegisterStaffUserService service;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        service = new RegisterStaffUserService(harness.users, harness.validateRole);
    }

    private StaffUser register(String id, String email, String document, UserRole role) {
        return service.register(DomainFixtures.administrator(), UserId.of(id), FullName.of("Staff " + id),
                EmailAddress.of(email), IdentityDocument.of(document), role);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"LOGISTICS_OPERATOR", "SUPERVISOR"})
    @DisplayName("incorporates an active staff user holding exactly the requested role")
    void incorporatesStaff(UserRole role) {
        StaffUser staff = register("staff-1", "staff-1@nexusmarket.test", "DOC-STAFF-1", role);

        assertEquals(role, staff.getRole());
        assertEquals(UserStatus.ACTIVE, staff.getStatus());
        assertInstanceOf(StaffUser.class, harness.users.findByIdentifier(UserId.of("staff-1")).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BUYER", "SELLER", "ADMINISTRATOR"})
    @DisplayName("rejects any role other than logistics operator or supervisor")
    void rejectsOtherRoles(UserRole role) {
        assertThrows(StaffRoleNotAllowedException.class,
                () -> register("staff-x", "x@nexusmarket.test", "DOC-X", role));
        assertTrue(harness.users.findByIdentifier(UserId.of("staff-x")).isEmpty());
    }

    @Test
    @DisplayName("rejects a taken identifier, email or document")
    void rejectsDuplicates() {
        register("staff-1", "staff-1@nexusmarket.test", "DOC-STAFF-1", UserRole.SUPERVISOR);

        assertThrows(DuplicateUserIdentifierException.class,
                () -> register("staff-1", "other@nexusmarket.test", "DOC-OTHER", UserRole.SUPERVISOR));
        assertThrows(DuplicateEmailException.class,
                () -> register("staff-2", "staff-1@nexusmarket.test", "DOC-OTHER", UserRole.SUPERVISOR));
        assertThrows(DuplicateIdentityDocumentException.class,
                () -> register("staff-3", "third@nexusmarket.test", "DOC-STAFF-1", UserRole.SUPERVISOR));
    }

    @Test
    @DisplayName("only an administrator incorporates staff")
    void onlyAdministrators() {
        assertThrows(UnauthorizedOperationException.class, () -> service.register(DomainFixtures.supervisor(),
                UserId.of("staff-9"), FullName.of("Staff"), EmailAddress.of("s9@nexusmarket.test"),
                IdentityDocument.of("DOC-9"), UserRole.SUPERVISOR));
        assertThrows(UnauthorizedOperationException.class, () -> service.register(DomainFixtures.buyer(),
                UserId.of("staff-9"), FullName.of("Staff"), EmailAddress.of("s9@nexusmarket.test"),
                IdentityDocument.of("DOC-9"), UserRole.SUPERVISOR));
    }
}
