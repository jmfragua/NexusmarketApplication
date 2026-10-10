package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.UserJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaUserRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdentityDocument(String identityDocument);

    List<UserJpaEntity> findByRole(String role);

    List<UserJpaEntity> findByUserType(String userType);
}
