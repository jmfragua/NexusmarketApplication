package application.infrastructure.config;

import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.authorization.ValidateUserActiveStatusService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the authorization domain services as beans. Every other area depends on these, so they
 * are the root of the service graph.
 */
@Configuration
public class AuthorizationServicesConfig {

    @Bean
    public ValidateUserActiveStatusService validateUserActiveStatusService() {
        return new ValidateUserActiveStatusService();
    }

    @Bean
    public ValidateRoleAuthorizationService validateRoleAuthorizationService(
            ValidateUserActiveStatusService validateUserActiveStatusService) {
        return new ValidateRoleAuthorizationService(validateUserActiveStatusService);
    }

    @Bean
    public ValidateBuyerOwnershipService validateBuyerOwnershipService() {
        return new ValidateBuyerOwnershipService();
    }

    @Bean
    public ValidateSellerOwnershipService validateSellerOwnershipService() {
        return new ValidateSellerOwnershipService();
    }
}
