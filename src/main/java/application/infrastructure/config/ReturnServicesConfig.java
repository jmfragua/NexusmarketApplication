package application.infrastructure.config;

import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.returns.ConsultReturnService;
import application.domain.services.returns.RequestReturnService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the after sales return domain services as beans.
 */
@Configuration
public class ReturnServicesConfig {

    @Bean
    public RequestReturnService requestReturnService(
            ProductReturnRepositoryPort productReturnRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new RequestReturnService(productReturnRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public ConsultReturnService consultReturnService(
            ProductReturnRepositoryPort productReturnRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ConsultReturnService(productReturnRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }
}
