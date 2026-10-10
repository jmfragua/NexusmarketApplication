package application.infrastructure.security.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredentialJpaEntity, String> {
}
