package application.infrastructure.security.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credential of a user, stored outside the domain.
 *
 * <p>The domain has no concept of a password: {@code User} declares no credential attribute and the
 * specification leaves authentication mechanisms out of scope (RD-ALC-01). Introducing a
 * {@code PasswordServicePort} would also break RD-POUT-01, since a credential is not an aggregate.
 * It therefore lives entirely here, in the security infrastructure.</p>
 */
@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
public class UserCredentialJpaEntity {

    @Id
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
}
