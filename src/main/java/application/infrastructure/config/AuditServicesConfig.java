package application.infrastructure.config;

import application.domain.ports.out.OperationLogRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the audit domain services as beans.
 *
 * <p>Domain services are plain Java classes with no framework annotation, which is what keeps the
 * domain free of Spring (RD-SRV-02). Their wiring therefore lives here, in infrastructure.</p>
 */
@Configuration
public class AuditServicesConfig {

    @Bean
    public RegisterOperationLogService registerOperationLogService(
            OperationLogRepositoryPort operationLogRepositoryPort) {
        return new RegisterOperationLogService(operationLogRepositoryPort);
    }
}
