package application.domain.valuesObjects;

import java.util.UUID;

/**
 * Identifier of an operation log entry. Distinct type from every other {@link Identifier}
 * specialization, so it cannot be used where another entity identifier is expected (RD-VO-03).
 *
 * <p>Unlike business aggregates, an operation log entry identifies a technical audit fact that no
 * use case references by identifier, so it is self-generated rather than supplied by the caller
 * (RD-AUD-02).</p>
 */
public final class OperationLogId extends Identifier {

    private OperationLogId(String value) {
        super(value);
    }

    public static OperationLogId of(String value) {
        return new OperationLogId(value);
    }

    public static OperationLogId generate() {
        return new OperationLogId(UUID.randomUUID().toString());
    }
}
