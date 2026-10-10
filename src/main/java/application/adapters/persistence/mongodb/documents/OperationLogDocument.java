package application.adapters.persistence.mongodb.documents;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persistence shape of an operation log entry.
 *
 * <p>Stored in MongoDB rather than in the relational schema because the audit trail is append-only,
 * holds no relations with the business aggregates and keeps a free-form {@code details} field —
 * exactly the profile a document store serves better than a table (RD-AUD-01).</p>
 */
@Document(collection = "operation_logs")
@Getter
@Setter
@NoArgsConstructor
public class OperationLogDocument {

    @Id
    private String id;

    private String operationType;

    private String actorId;

    private String actorRole;

    private String affectedEntityType;

    private String affectedEntityId;

    private String details;

    private Instant occurredAt;
}
