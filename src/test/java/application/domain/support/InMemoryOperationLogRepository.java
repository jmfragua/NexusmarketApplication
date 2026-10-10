package application.domain.support;

import application.domain.models.OperationLog;
import application.domain.ports.out.OperationLogRepositoryPort;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory double of {@link OperationLogRepositoryPort}.
 *
 * <p>Storing what it receives is what lets the tests assert that an operation really was audited —
 * and, just as importantly, that the ones outside the agreed set were not (RD-AUD-01).</p>
 */
public class InMemoryOperationLogRepository implements OperationLogRepositoryPort {

    private final Map<String, OperationLog> logs = new LinkedHashMap<>();

    @Override
    public OperationLog save(OperationLog operationLog) {
        logs.put(operationLog.getIdentifier().getValue(), operationLog);
        return operationLog;
    }

    @Override
    public Optional<OperationLog> findById(OperationLogId operationLogId) {
        return Optional.ofNullable(logs.get(operationLogId.getValue()));
    }

    @Override
    public List<OperationLog> findByOperationType(OperationType operationType) {
        return logs.values().stream()
                .filter(log -> log.getOperationType() == operationType)
                .toList();
    }

    @Override
    public List<OperationLog> findByActor(Identifier actorId) {
        return logs.values().stream()
                .filter(log -> log.getActorId().equals(actorId))
                .toList();
    }

    @Override
    public List<OperationLog> findAll() {
        return new ArrayList<>(logs.values());
    }

    public int size() {
        return logs.size();
    }

    /**
     * Empties the log. Needed when this double is wired into a Spring context, where it lives as a
     * singleton and would otherwise carry entries from one test into the next — a transaction
     * rollback does not reach in-memory state.
     */
    public void clear() {
        logs.clear();
    }
}
