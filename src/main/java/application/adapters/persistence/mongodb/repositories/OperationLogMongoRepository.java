package application.adapters.persistence.mongodb.repositories;

import application.adapters.persistence.mongodb.documents.OperationLogDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OperationLogMongoRepository extends MongoRepository<OperationLogDocument, String> {

    List<OperationLogDocument> findByOperationType(String operationType);

    List<OperationLogDocument> findByActorId(String actorId);
}
