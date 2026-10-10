package application.domain.services.audit;

import application.domain.models.DomainEntity;
import application.domain.models.OperationLog;
import application.domain.models.User;
import application.domain.ports.out.OperationLogRepositoryPort;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import java.time.Instant;
import java.util.Objects;

/**
 * Registers an operation log entry (RD-AUD-01).
 *
 * <p>Unlike the rest of the domain services, this one verifies no business rule: any invalid input
 * is a programming error, signalled by the guards already present in {@link OperationLog} and its
 * value objects, never by a {@link application.domain.exceptions.DomainException}.</p>
 */
public class RegisterOperationLogService {

    private final OperationLogRepositoryPort operationLogRepositoryPort;

    public RegisterOperationLogService(OperationLogRepositoryPort operationLogRepositoryPort) {
        this.operationLogRepositoryPort = Objects.requireNonNull(operationLogRepositoryPort,
                "the operation log repository is mandatory");
    }

    /**
     * @param actor           user who performed the operation.
     * @param operationType   category of the operation.
     * @param affectedEntity  entity the operation acted upon.
     * @param details         short, human readable description of what changed.
     * @return the registered log entry.
     */
    public OperationLog register(User<?> actor, OperationType operationType, DomainEntity<?> affectedEntity, String details) {
        OperationLog log = OperationLog.register(OperationLogId.generate(), operationType, actor,
                affectedEntity, details, Instant.now());
        return operationLogRepositoryPort.save(log);
    }
}
