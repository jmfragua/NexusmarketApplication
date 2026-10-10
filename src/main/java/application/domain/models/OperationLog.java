package application.domain.models;

import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.UserRole;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Immutable record of a relevant business operation, kept for traceability and audit (RD-AUD-01).
 *
 * <p>An operation log entry owns no lifecycle of its own: it is an append-only fact, never updated
 * or transitioned once registered, unlike the rest of the domain entities.</p>
 */
@Getter
public class OperationLog extends DomainEntity<OperationLogId> {

    private final OperationType operationType;

    private final Identifier actorId;

    private final UserRole actorRole;

    private final String affectedEntityType;

    private final String affectedEntityId;

    private final String details;

    private final Instant occurredAt;

    private OperationLog(OperationLogId identifier,
                         OperationType operationType,
                         Identifier actorId,
                         UserRole actorRole,
                         String affectedEntityType,
                         String affectedEntityId,
                         String details,
                         Instant occurredAt) {
        super(identifier);
        this.operationType = Objects.requireNonNull(operationType, "operation type is mandatory");
        this.actorId = Objects.requireNonNull(actorId, "the identifier of the actor is mandatory");
        this.actorRole = Objects.requireNonNull(actorRole, "the role of the actor is mandatory");
        this.affectedEntityType = Objects.requireNonNull(affectedEntityType, "the affected entity type is mandatory");
        this.affectedEntityId = Objects.requireNonNull(affectedEntityId, "the affected entity identifier is mandatory");
        this.details = Objects.requireNonNull(details, "details are mandatory");
        this.occurredAt = Objects.requireNonNull(occurredAt, "the moment of the operation is mandatory");
    }

    /**
     * Registers a new log entry out of the user who performed the operation and the entity it
     * affected (RD-AUD-01).
     */
    public static OperationLog register(OperationLogId identifier,
                                        OperationType operationType,
                                        User<?> actor,
                                        DomainEntity<?> affectedEntity,
                                        String details,
                                        Instant occurredAt) {
        Objects.requireNonNull(actor, "the actor is mandatory");
        Objects.requireNonNull(affectedEntity, "the affected entity is mandatory");
        return new OperationLog(identifier, operationType, actor.getIdentifier(), actor.getRole(),
                affectedEntity.getClass().getSimpleName(), affectedEntity.getIdentifier().getValue(),
                details, occurredAt);
    }

    /**
     * Reconstructs a previously persisted log entry (RD-ALC-04). An audit fact is never recreated
     * through business validations once it already happened.
     */
    public static OperationLog reconstruct(OperationLogId identifier,
                                           OperationType operationType,
                                           Identifier actorId,
                                           UserRole actorRole,
                                           String affectedEntityType,
                                           String affectedEntityId,
                                           String details,
                                           Instant occurredAt) {
        return new OperationLog(identifier, operationType, actorId, actorRole, affectedEntityType,
                affectedEntityId, details, occurredAt);
    }
}
