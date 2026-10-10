package application.infrastructure.seed;

import application.domain.models.StaffUser;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import application.infrastructure.security.CredentialService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator when the platform has none.
 *
 * <p>Without it the system is unreachable: only buyers self register, sellers are incorporated by
 * an administrator (RD-ROL-05), and there is no other way to bring the first one into existence.
 * Catalogue, inventory and orders would all stay out of reach.</p>
 *
 * <p>It is idempotent — it does nothing when an administrator already exists — so restarting the
 * application never duplicates or resets anything.</p>
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepositoryPort userRepositoryPort;

    private final CredentialService credentialService;

    private final String adminId;

    private final String adminEmail;

    private final String adminPassword;

    private final String adminDocument;

    public DatabaseSeeder(UserRepositoryPort userRepositoryPort,
                          CredentialService credentialService,
                          @Value("${nexusmarket.seed.admin.id:admin-001}") String adminId,
                          @Value("${nexusmarket.seed.admin.email:admin@nexusmarket.local}") String adminEmail,
                          @Value("${nexusmarket.seed.admin.password:}") String adminPassword,
                          @Value("${nexusmarket.seed.admin.document:ADMIN-0001}") String adminDocument) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort);
        this.credentialService = Objects.requireNonNull(credentialService);
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminDocument = adminDocument;
    }

    @Override
    public void run(String... args) {
        if (!userRepositoryPort.findByRole(UserRole.ADMINISTRATOR).isEmpty()) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            LOGGER.warn("No administrator exists and no seed password is configured. "
                    + "Set nexusmarket.seed.admin.password (or SEED_ADMIN_PASSWORD) to bootstrap one.");
            return;
        }
        StaffUser administrator = StaffUser.of(
                UserId.of(adminId),
                FullName.of("Administrador inicial"),
                EmailAddress.of(adminEmail),
                IdentityDocument.of(adminDocument),
                UserRole.ADMINISTRATOR,
                UserStatus.ACTIVE);
        userRepositoryPort.save(administrator);
        credentialService.register(administrator.getEmail(), adminPassword);
        LOGGER.info("Initial administrator created with email {}", adminEmail);
    }
}
