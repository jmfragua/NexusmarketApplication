package application.infrastructure.config;

import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.invoice.ConsultInvoiceService;
import application.domain.services.invoice.IssueInvoiceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the invoicing domain services as beans.
 */
@Configuration
public class InvoiceServicesConfig {

    @Bean
    public IssueInvoiceService issueInvoiceService(
            InvoiceRepositoryPort invoiceRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new IssueInvoiceService(invoiceRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ConsultInvoiceService consultInvoiceService(
            InvoiceRepositoryPort invoiceRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ConsultInvoiceService(invoiceRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }
}
