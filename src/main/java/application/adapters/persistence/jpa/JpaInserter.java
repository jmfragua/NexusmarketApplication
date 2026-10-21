package application.adapters.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts a new row, never updating an existing one.
 *
 * <p>{@code JpaRepository.save} merges whenever the entity carries an identifier, and every
 * identifier here is supplied by the client (RD-POUT-04): a save of a new entity whose identifier is
 * taken silently overwrites the existing row. {@code persist} issues an {@code INSERT}, so the
 * primary and unique keys of the database refuse a duplicate even when two requests race past the
 * check of the domain. Flushing here surfaces that refusal inside this {@code @Repository}, which
 * translates it into a {@code DataIntegrityViolationException}.</p>
 */
@Repository
public class JpaInserter {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void insert(Object entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }
}
