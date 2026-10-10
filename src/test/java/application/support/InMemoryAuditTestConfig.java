package application.support;

import application.domain.support.InMemoryOperationLogRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the MongoDB audit adapter with an in-memory double during integration tests.
 *
 * <p>Without this, any test that triggers an audited operation — confirming a payment, incorporating
 * a seller — would try to reach a real MongoDB. The substitution keeps the tests self-contained and
 * fast while still letting them assert what was written to the log (RD-AUD-01).</p>
 */
@TestConfiguration
public class InMemoryAuditTestConfig {

    @Bean
    @Primary
    public InMemoryOperationLogRepository inMemoryOperationLogRepository() {
        return new InMemoryOperationLogRepository();
    }
}
