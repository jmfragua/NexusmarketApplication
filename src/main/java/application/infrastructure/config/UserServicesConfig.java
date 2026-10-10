package application.infrastructure.config;

import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateUserActiveStatusService;
import application.domain.services.user.ActivateUserService;
import application.domain.services.user.BlockUserService;
import application.domain.services.user.ChangeUserRoleService;
import application.domain.services.user.LoginService;
import application.domain.services.user.RegisterBuyerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the user domain services as beans.
 */
@Configuration
public class UserServicesConfig {

    @Bean
    public RegisterBuyerService registerBuyerService(UserRepositoryPort userRepositoryPort,
                                                     CartRepositoryPort cartRepositoryPort) {
        return new RegisterBuyerService(userRepositoryPort, cartRepositoryPort);
    }

    @Bean
    public BlockUserService blockUserService(UserRepositoryPort userRepositoryPort,
                                             ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new BlockUserService(userRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ActivateUserService activateUserService(UserRepositoryPort userRepositoryPort,
                                                   ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ActivateUserService(userRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public ChangeUserRoleService changeUserRoleService(
            UserRepositoryPort userRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ChangeUserRoleService(userRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public LoginService loginService(UserRepositoryPort userRepositoryPort,
                                     ValidateUserActiveStatusService validateUserActiveStatusService) {
        return new LoginService(userRepositoryPort, validateUserActiveStatusService);
    }
}
