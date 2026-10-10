package application.infrastructure.config;

import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.cart.AddCartItemService;
import application.domain.services.cart.ClearCartService;
import application.domain.services.cart.ConfirmCartService;
import application.domain.services.cart.RemoveCartItemService;
import application.domain.services.cart.UpdateCartItemQuantityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the cart domain services as beans.
 */
@Configuration
public class CartServicesConfig {

    @Bean
    public AddCartItemService addCartItemService(
            CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new AddCartItemService(cartRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public RemoveCartItemService removeCartItemService(
            CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new RemoveCartItemService(cartRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public UpdateCartItemQuantityService updateCartItemQuantityService(
            CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new UpdateCartItemQuantityService(cartRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public ClearCartService clearCartService(
            CartRepositoryPort cartRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ClearCartService(cartRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public ConfirmCartService confirmCartService(
            CartRepositoryPort cartRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ConfirmCartService(cartRepositoryPort, orderRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }
}
