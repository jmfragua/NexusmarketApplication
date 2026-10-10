package application.infrastructure.config;

import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.reporting.ConsultInventoryReportService;
import application.domain.services.reporting.ConsultSalesReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the administrative reporting domain services as beans. They consume ports of several
 * aggregates without owning one of their own: reporting is not an entity (OBJ-12).
 */
@Configuration
public class ReportingServicesConfig {

    @Bean
    public ConsultSalesReportService consultSalesReportService(
            OrderRepositoryPort orderRepositoryPort,
            InvoiceRepositoryPort invoiceRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultSalesReportService(orderRepositoryPort, invoiceRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public ConsultInventoryReportService consultInventoryReportService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultInventoryReportService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }
}
