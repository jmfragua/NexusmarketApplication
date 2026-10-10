package application.infrastructure.config;

import application.domain.ports.out.OrderRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.order.CompleteDeliveryService;
import application.domain.services.order.ConfirmPaymentService;
import application.domain.services.order.ConsultOrderService;
import application.domain.services.order.DispatchOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the order domain services as beans. The three transitions also receive the audit service,
 * because the order lifecycle is logged (RD-AUD-01).
 */
@Configuration
public class OrderServicesConfig {

    @Bean
    public ConfirmPaymentService confirmPaymentService(
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService,
            RegisterOperationLogService registerOperationLogService) {
        return new ConfirmPaymentService(orderRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService, registerOperationLogService);
    }

    @Bean
    public DispatchOrderService dispatchOrderService(
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationLogService registerOperationLogService) {
        return new DispatchOrderService(orderRepositoryPort, validateRoleAuthorizationService,
                registerOperationLogService);
    }

    @Bean
    public CompleteDeliveryService completeDeliveryService(
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationLogService registerOperationLogService) {
        return new CompleteDeliveryService(orderRepositoryPort, validateRoleAuthorizationService,
                registerOperationLogService);
    }

    @Bean
    public ConsultOrderService consultOrderService(
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ConsultOrderService(orderRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }
}
