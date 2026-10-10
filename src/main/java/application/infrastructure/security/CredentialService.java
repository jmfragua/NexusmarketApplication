package application.infrastructure.security;

import application.domain.valuesObjects.EmailAddress;
import application.infrastructure.security.persistence.UserCredentialJpaEntity;
import application.infrastructure.security.persistence.UserCredentialRepository;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Stores and verifies user credentials, hashed with BCrypt.
 *
 * <p>Deliberately not a domain service: it manages a concept the domain does not model
 * (RD-ALC-01).</p>
 */
@Service
public class CredentialService {

    private final UserCredentialRepository credentialRepository;

    private final PasswordEncoder passwordEncoder;

    public CredentialService(UserCredentialRepository credentialRepository, PasswordEncoder passwordEncoder) {
        this.credentialRepository = Objects.requireNonNull(credentialRepository,
                "the credential repository is mandatory");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "the password encoder is mandatory");
    }

    /**
     * Registers the credential of a user, storing only the hash — never the raw password.
     */
    public void register(EmailAddress email, String rawPassword) {
        Objects.requireNonNull(email, "the email address is mandatory");
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("the password is mandatory and must not be blank");
        }
        UserCredentialJpaEntity credential = new UserCredentialJpaEntity();
        credential.setEmail(email.getValue());
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credentialRepository.save(credential);
    }

    /**
     * @return true when the raw password matches the stored hash of that email.
     */
    public boolean matches(EmailAddress email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return false;
        }
        return credentialRepository.findById(email.getValue())
                .map(credential -> passwordEncoder.matches(rawPassword, credential.getPasswordHash()))
                .orElse(false);
    }
}
