package application.infrastructure.config;

import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.warehouse.ConsultWarehouseService;
import application.domain.services.warehouse.RegisterMarketplaceWarehouseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the warehouse domain services as beans.
 */
@Configuration
public class WarehouseServicesConfig {

    @Bean
    public RegisterMarketplaceWarehouseService registerMarketplaceWarehouseService(
            WarehouseRepositoryPort warehouseRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new RegisterMarketplaceWarehouseService(warehouseRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ConsultWarehouseService consultWarehouseService(
            WarehouseRepositoryPort warehouseRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new ConsultWarehouseService(warehouseRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }
}
