package application.infrastructure.config;

import application.domain.ports.out.RefundRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.refund.ConsultRefundService;
import application.domain.services.refund.IssueRefundService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the refund domain services as beans.
 */
@Configuration
public class RefundServicesConfig {

    @Bean
    public IssueRefundService issueRefundService(
            RefundRepositoryPort refundRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationLogService registerOperationLogService) {
        return new IssueRefundService(refundRepositoryPort, validateRoleAuthorizationService,
                registerOperationLogService);
    }

    @Bean
    public ConsultRefundService consultRefundService(
            RefundRepositoryPort refundRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultRefundService(refundRepositoryPort, validateRoleAuthorizationService);
    }
}
