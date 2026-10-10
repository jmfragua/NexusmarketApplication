package application.adapters.persistence.mongodb.mappers;

import application.adapters.persistence.mongodb.documents.OperationLogDocument;
import application.domain.models.OperationLog;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;

/**
 * Translates an operation log entry between its domain shape and its document shape.
 *
 * <p>Restored through {@link OperationLog#reconstruct} rather than {@code register}: the latter
 * needs the live actor and affected entity to derive their identifiers, which a reload does not
 * have — and should not fetch, since the entry must stay a faithful snapshot of what was true when
 * the operation happened (RD-ALC-04).</p>
 */
public final class OperationLogMongoMapper {

    private OperationLogMongoMapper() {
    }

    public static OperationLogDocument toDocument(OperationLog operationLog) {
        OperationLogDocument document = new OperationLogDocument();
        document.setId(operationLog.getIdentifier().getValue());
        document.setOperationType(operationLog.getOperationType().code());
        document.setActorId(operationLog.getActorId().getValue());
        document.setActorRole(operationLog.getActorRole().code());
        document.setAffectedEntityType(operationLog.getAffectedEntityType());
        document.setAffectedEntityId(operationLog.getAffectedEntityId());
        document.setDetails(operationLog.getDetails());
        document.setOccurredAt(operationLog.getOccurredAt());
        return document;
    }

    public static OperationLog toDomain(OperationLogDocument document) {
        UserRole actorRole = UserRole.valueOf(document.getActorRole());
        return OperationLog.reconstruct(
                OperationLogId.of(document.getId()),
                OperationType.valueOf(document.getOperationType()),
                actorIdentifierOf(actorRole, document.getActorId()),
                actorRole,
                document.getAffectedEntityType(),
                document.getAffectedEntityId(),
                document.getDetails(),
                document.getOccurredAt());
    }

    /**
     * Rebuilds the identifier of the actor with the specialization its role implies.
     *
     * <p>The concrete type matters: identifiers compare by value <em>and</em> by type (RD-VO-03), so
     * restoring a buyer as a generic identifier would make {@code findByActor(buyerId)} silently
     * match nothing. The stored role is what tells the specializations apart.</p>
     */
    private static Identifier actorIdentifierOf(UserRole actorRole, String actorId) {
        return switch (actorRole) {
            case BUYER -> BuyerId.of(actorId);
            case SELLER -> SellerId.of(actorId);
            case LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR -> UserId.of(actorId);
        };
    }
}
