package application.adapters.persistence.mongodb;

import application.adapters.persistence.mongodb.mappers.OperationLogMongoMapper;
import application.adapters.persistence.mongodb.repositories.OperationLogMongoRepository;
import application.domain.models.OperationLog;
import application.domain.ports.out.OperationLogRepositoryPort;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.OperationLogId;
import application.domain.valuesObjects.OperationType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Document adapter of {@link OperationLogRepositoryPort}, the only port not backed by the relational
 * schema.
 */
@Repository
public class OperationLogMongoAdapter implements OperationLogRepositoryPort {

    private final OperationLogMongoRepository operationLogRepository;

    public OperationLogMongoAdapter(OperationLogMongoRepository operationLogRepository) {
        this.operationLogRepository = Objects.requireNonNull(operationLogRepository,
                "the operation log repository is mandatory");
    }

    @Override
    public OperationLog save(OperationLog operationLog) {
        operationLogRepository.save(OperationLogMongoMapper.toDocument(operationLog));
        return operationLog;
    }

    @Override
    public Optional<OperationLog> findById(OperationLogId operationLogId) {
        return operationLogRepository.findById(operationLogId.getValue())
                .map(OperationLogMongoMapper::toDomain);
    }

    @Override
    public List<OperationLog> findByOperationType(OperationType operationType) {
        return operationLogRepository.findByOperationType(operationType.code()).stream()
                .map(OperationLogMongoMapper::toDomain)
                .toList();
    }

    @Override
    public List<OperationLog> findByActor(Identifier actorId) {
        return operationLogRepository.findByActorId(actorId.getValue()).stream()
                .map(OperationLogMongoMapper::toDomain)
                .toList();
    }

    @Override
    public List<OperationLog> findAll() {
        return operationLogRepository.findAll().stream()
                .map(OperationLogMongoMapper::toDomain)
                .toList();
    }
}
