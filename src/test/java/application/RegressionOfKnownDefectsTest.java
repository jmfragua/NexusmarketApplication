package application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.InvalidRoleChangeException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.OperationLog;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.ports.in.AdministratorPort;
import application.domain.ports.in.LogisticOperatorPort;
import application.domain.ports.in.PublicAccessPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.support.InMemoryOperationLogRepository;
import application.domain.support.DomainFixtures;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import application.infrastructure.seed.DatabaseSeeder;
import application.infrastructure.security.CredentialService;
import application.support.InMemoryAuditTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks down the defects that reached a running system and were only found by exercising it end to
 * end against real databases.
 *
 * <p>The first four slipped past 77 unit tests for the same reason: those tests built their own
 * aggregates with fixtures and never asked where the <em>first</em> one comes from. The fifth
 * slipped past for a sibling reason — the tests never <em>reloaded</em> what they had saved, which
 * is the only moment the discriminator gets to contradict a changed column.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(InMemoryAuditTestConfig.class)
@Transactional
class RegressionOfKnownDefectsTest {

    @Autowired
    private PublicAccessPort publicAccessPort;

    @Autowired
    private LogisticOperatorPort logisticOperatorPort;

    @Autowired
    private AdministratorPort administratorPort;

    @Autowired
    private CartRepositoryPort cartRepositoryPort;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private InMemoryOperationLogRepository operationLogRepositoryPort;

    @BeforeEach
    void resetInMemoryState() {
        operationLogRepositoryPort.clear();
    }

    @Autowired
    private CredentialService credentialService;

    @Nested
    @DisplayName("Defect 1: the buyer had no cart and nothing created one")
    class BuyerCartIsCreatedOnRegistration {

        /**
         * The original symptom: {@code GET /buyer/me/cart} answered 404 forever, so no product could
         * be selected and no order could ever be placed. The entire purchase flow was unreachable.
         */
        @Test
        @DisplayName("registering a buyer leaves them with an active cart")
        void registrationCreatesTheCart() {
            Buyer buyer = DomainFixtures.buyer("buyer-reg-1", UserStatus.ACTIVE,
                    application.domain.valuesObjects.BuyerCommercialStatus.ENABLED);

            publicAccessPort.registerBuyer(buyer, CartId.of("cart-reg-1"));

            Cart cart = cartRepositoryPort.findByBuyer(BuyerId.of("buyer-reg-1")).orElseThrow(
                    () -> new AssertionError("the buyer must hold an active cart right after registration"));
            assertEquals("buyer-reg-1", cart.getBuyerId().getValue());
            assertTrue(cart.isEmpty(), "a brand new cart starts empty");
        }

        @Test
        @DisplayName("the cart is reachable by the buyer identifier, which is how the port looks it up")
        void cartIsReachableByBuyer() {
            Buyer buyer = DomainFixtures.buyer("buyer-reg-2", UserStatus.ACTIVE,
                    application.domain.valuesObjects.BuyerCommercialStatus.ENABLED);
            publicAccessPort.registerBuyer(buyer, CartId.of("cart-reg-2"));

            assertTrue(cartRepositoryPort.findByBuyer(buyer.getIdentifier()).isPresent());
        }
    }

    @Nested
    @DisplayName("Defect 2: no way to create the first administrator")
    class SeederBootstrapsTheFirstAdministrator {

        private DatabaseSeeder seederWithPassword(String password) {
            return new DatabaseSeeder(userRepositoryPort, credentialService,
                    "admin-seed", "seed@nexusmarket.test", password, "DOC-SEED");
        }

        @Test
        @DisplayName("creates an administrator when the platform has none")
        void createsAdministratorOnEmptyPlatform() {
            assertTrue(userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).isEmpty());

            seederWithPassword("una-clave").run();

            assertEquals(1, userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).size());
        }

        @Test
        @DisplayName("is idempotent: a second run creates nothing")
        void doesNotDuplicateOnSecondRun() {
            seederWithPassword("una-clave").run();
            seederWithPassword("una-clave").run();

            assertEquals(1, userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).size(),
                    "restarting the application must never duplicate the administrator");
        }

        /**
         * A default password in source code would be a known credential in every deployment that
         * forgot to change it, so the seeder refuses to act instead (RD-BOOT-02).
         */
        @Test
        @DisplayName("refuses to seed without a configured password")
        void refusesWithoutPassword() {
            seederWithPassword("").run();

            assertTrue(userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).isEmpty());
        }

        @Test
        @DisplayName("the seeded administrator can authenticate with the configured password")
        void seededAdministratorCanAuthenticate() {
            seederWithPassword("clave-de-arranque").run();

            EmailAddress email = EmailAddress.of("seed@nexusmarket.test");
            assertTrue(credentialService.matches(email, "clave-de-arranque"));
            assertFalse(credentialService.matches(email, "otra-clave"));
            assertEquals(UserRole.ADMINISTRATOR, publicAccessPort.login(email).getRole());
        }
    }

    @Nested
    @DisplayName("Defect 3: three of the five roles could not be instantiated")
    class StaffUserMakesRemainingRolesPossible {

        private StaffUser staffOf(UserRole role) {
            return StaffUser.of(UserId.of("staff-" + role.code()),
                    FullName.of("Staff"),
                    EmailAddress.of(role.code().toLowerCase() + "@nexusmarket.test"),
                    IdentityDocument.of("DOC-" + role.code()),
                    role,
                    UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("the three roles without specialization can exist as objects")
        void rolesWithoutSpecializationCanExist() {
            assertEquals(UserRole.ADMINISTRATOR, staffOf(UserRole.ADMINISTRATOR).getRole());
            assertEquals(UserRole.LOGISTICS_OPERATOR, staffOf(UserRole.LOGISTICS_OPERATOR).getRole());
            assertEquals(UserRole.SUPERVISOR, staffOf(UserRole.SUPERVISOR).getRole());
        }

        /**
         * Building a buyer or a seller through this path would bypass their own creation rules — a
         * commercial status, or an incorporation that only an administrator may perform (RD-ROL-05).
         */
        @Test
        @DisplayName("refuses the roles that own a specialization of their own")
        void refusesSpecializedRoles() {
            assertThrows(IllegalArgumentException.class, () -> staffOf(UserRole.BUYER));
            assertThrows(IllegalArgumentException.class, () -> staffOf(UserRole.SELLER));
        }

        @Test
        @DisplayName("round-trips through persistence keeping its role")
        void roundTripsThroughPersistence() {
            userRepositoryPort.save(staffOf(UserRole.LOGISTICS_OPERATOR));

            User<?> reloaded = userRepositoryPort
                    .findByEmail(EmailAddress.of("logistics_operator@nexusmarket.test")).orElseThrow();

            assertEquals(UserRole.LOGISTICS_OPERATOR, reloaded.getRole());
            assertTrue(reloaded instanceof StaffUser, "the stored discriminator must rebuild a StaffUser");
        }
    }

    @Nested
    @DisplayName("Defect 4: the audit log identified its actor with the wrong identifier type")
    class AuditLogKeepsTheActorIdentifierType {

        /**
         * Identifiers compare by value <em>and</em> by concrete type (RD-VO-03). Rebuilding a buyer's
         * identifier as a generic one compiles fine and silently makes {@code findByActor} match
         * nothing at all.
         */
        @Test
        @DisplayName("a log entry is retrievable by the actor identifier that produced it")
        void retrievableByActorIdentifier() {
            Buyer buyer = DomainFixtures.buyer();
            OperationLog log = OperationLog.register(
                    application.domain.valuesObjects.OperationLogId.generate(),
                    OperationType.ORDER_STATUS_CHANGED,
                    buyer,
                    DomainFixtures.orderIn(buyer, application.domain.valuesObjects.OrderStatus.PAID),
                    "order moved to PAID",
                    java.time.Instant.now());
            operationLogRepositoryPort.save(log);

            assertEquals(1, operationLogRepositoryPort.findByActor(buyer.getIdentifier()).size(),
                    "the buyer identifier must match the one stored in the log entry");
        }

        @Test
        @DisplayName("the entry carries the role the actor had at that moment")
        void keepsTheRoleOfTheMoment() {
            Buyer buyer = DomainFixtures.buyer();
            operationLogRepositoryPort.save(OperationLog.register(
                    application.domain.valuesObjects.OperationLogId.generate(),
                    OperationType.ORDER_STATUS_CHANGED,
                    buyer,
                    DomainFixtures.orderIn(buyer, application.domain.valuesObjects.OrderStatus.PAID),
                    "order moved to PAID",
                    java.time.Instant.now()));

            assertEquals(UserRole.BUYER,
                    operationLogRepositoryPort.findAll().get(0).getActorRole());
        }
    }

    @Nested
    @DisplayName("Defect 5: changing a role across specializations was accepted and then lost")
    class RoleChangeCannotCrossSpecializations {

        /**
         * The reassignment reported success and the response echoed the new role, but the stored
         * discriminator still said {@code BUYER}, so the next reload rebuilt a {@code Buyer} whose
         * constructor hardcodes {@code UserRole.BUYER} and threw the change away. The user kept
         * acting as a buyer while the column claimed otherwise.
         */
        @Test
        @DisplayName("a buyer cannot be turned into staff by reassigning the attribute")
        void buyerCannotBecomeStaff() {
            Buyer buyer = DomainFixtures.buyer();

            assertThrows(InvalidRoleChangeException.class,
                    () -> buyer.changeRole(UserRole.LOGISTICS_OPERATOR));
            assertThrows(InvalidRoleChangeException.class, () -> buyer.changeRole(UserRole.ADMINISTRATOR));
            assertEquals(UserRole.BUYER, buyer.getRole(), "the rejected change must leave the role untouched");
        }

        /**
         * The reverse direction is just as impossible: a seller only exists incorporated by an
         * administrator together with their first warehouse (RD-ROL-05).
         */
        @Test
        @DisplayName("staff cannot be turned into a seller either")
        void staffCannotBecomeSeller() {
            StaffUser supervisor = StaffUser.of(UserId.of("sv-1"), FullName.of("Sup"),
                    EmailAddress.of("sv1@nexusmarket.test"), IdentityDocument.of("DOC-SV1"),
                    UserRole.SUPERVISOR, UserStatus.ACTIVE);

            assertThrows(InvalidRoleChangeException.class, () -> supervisor.changeRole(UserRole.SELLER));
            assertThrows(InvalidRoleChangeException.class, () -> supervisor.changeRole(UserRole.BUYER));
        }

        @Test
        @DisplayName("reassignment among the three staff roles is allowed and survives a reload")
        void staffRolesAreInterchangeable() {
            StaffUser staff = StaffUser.of(UserId.of("st-1"), FullName.of("Staff"),
                    EmailAddress.of("st1@nexusmarket.test"), IdentityDocument.of("DOC-ST1"),
                    UserRole.SUPERVISOR, UserStatus.ACTIVE);

            staff.changeRole(UserRole.LOGISTICS_OPERATOR);
            userRepositoryPort.save(staff);

            User<?> reloaded = userRepositoryPort.findByEmail(EmailAddress.of("st1@nexusmarket.test"))
                    .orElseThrow();
            assertEquals(UserRole.LOGISTICS_OPERATOR, reloaded.getRole(),
                    "the reassignment must survive the round trip, not only the in-memory object");
        }

        @Test
        @DisplayName("reassigning the same role is a no-op, not a rejection")
        void sameRoleIsAccepted() {
            Buyer buyer = DomainFixtures.buyer();

            buyer.changeRole(UserRole.BUYER);

            assertEquals(UserRole.BUYER, buyer.getRole());
        }
    }

    @Nested
    @DisplayName("Defect 6: resolving by identifier leaked existence before checking the role")
    class ResolutionChecksTheRoleFirst {

        /**
         * The identifier-based entries of the ports resolve the entity from a repository. Running the
         * role check after the lookup let an unauthorized actor tell a 404 from a 403 and so learn
         * whether the entity exists (RD-ROL-03). The check has to come first.
         */
        @Test
        @DisplayName("an unauthorized role is rejected before the entity is looked up")
        void unauthorizedRoleIsRejectedBeforeTheLookup() {
            Buyer buyer = DomainFixtures.buyer();

            assertThrows(UnauthorizedOperationException.class,
                    () -> logisticOperatorPort.consultOrder(buyer, OrderId.of("does-not-exist")),
                    "a buyer must be rejected for the role, not answered that the order is missing");
            assertThrows(UnauthorizedOperationException.class,
                    () -> logisticOperatorPort.consultStockRecord(buyer, InventoryItemId.of("nope")));
        }

        @Test
        @DisplayName("the same holds for the identifier-based shipment entries")
        void shipmentEntriesCheckTheRoleFirst() {
            Buyer buyer = DomainFixtures.buyer();

            assertThrows(UnauthorizedOperationException.class, () -> logisticOperatorPort
                    .dispatchShipment(buyer, ShipmentId.of("sh-x"), OrderId.of("does-not-exist")));
            assertThrows(UnauthorizedOperationException.class, () -> logisticOperatorPort
                    .packShipment(buyer, ShipmentId.of("sh-x"), OrderId.of("does-not-exist"), null, null));
        }

        /**
         * Invoicing belongs to the seller and the administrator (RD-FAC-01), so a buyer reaching it
         * through the administrator port must not find out whether the order exists either.
         */
        @Test
        @DisplayName("issuing an invoice checks the role before resolving the order")
        void invoicingChecksTheRoleFirst() {
            Buyer buyer = DomainFixtures.buyer();

            assertThrows(UnauthorizedOperationException.class, () -> administratorPort
                    .issueInvoice(buyer, OrderId.of("does-not-exist"), InvoiceId.of("inv-x")));
        }
    }
}
