package application.domain.ports.out;

import application.domain.models.OperationLog;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import java.util.List;
import java.util.Optional;

/**
 * Output port of the operation log. Every relevant business operation is kept as an immutable,
 * append-only fact for traceability and audit (RD-AUD-01).
 */
public interface OperationLogRepositoryPort {

    OperationLog save(OperationLog operationLog);

    Optional<OperationLog> findById(OperationLogId operationLogId);

    List<OperationLog> findByOperationType(OperationType operationType);

    List<OperationLog> findByActor(Identifier actorId);

    List<OperationLog> findAll();
}
