package application.infrastructure.config;

import application.domain.ports.out.ProductRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.catalog.AddProductVariantService;
import application.domain.services.catalog.ConsultCatalogService;
import application.domain.services.catalog.DiscontinueProductService;
import application.domain.services.catalog.PublishProductService;
import application.domain.services.catalog.RegisterProductService;
import application.domain.services.catalog.RemoveProductVariantService;
import application.domain.services.catalog.SuspendProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the catalogue domain services as beans.
 */
@Configuration
public class CatalogServicesConfig {

    @Bean
    public RegisterProductService registerProductService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new RegisterProductService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public PublishProductService publishProductService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new PublishProductService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public SuspendProductService suspendProductService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new SuspendProductService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public DiscontinueProductService discontinueProductService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new DiscontinueProductService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public AddProductVariantService addProductVariantService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new AddProductVariantService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public RemoveProductVariantService removeProductVariantService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new RemoveProductVariantService(productRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public ConsultCatalogService consultCatalogService(
            ProductRepositoryPort productRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultCatalogService(productRepositoryPort, validateRoleAuthorizationService);
    }
}
