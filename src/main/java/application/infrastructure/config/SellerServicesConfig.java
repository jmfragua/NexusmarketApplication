package application.infrastructure.config;

import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.seller.ConsultSellerService;
import application.domain.services.seller.RegisterSellerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the seller domain services as beans.
 */
@Configuration
public class SellerServicesConfig {

    @Bean
    public RegisterSellerService registerSellerService(
            SellerRepositoryPort sellerRepositoryPort,
            WarehouseRepositoryPort warehouseRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationLogService registerOperationLogService) {
        return new RegisterSellerService(sellerRepositoryPort, warehouseRepositoryPort, userRepositoryPort,
                validateRoleAuthorizationService, registerOperationLogService);
    }

    @Bean
    public ConsultSellerService consultSellerService(
            SellerRepositoryPort sellerRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultSellerService(sellerRepositoryPort, validateRoleAuthorizationService);
    }
}
