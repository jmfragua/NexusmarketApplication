package application.infrastructure.config;

import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.inventory.AdjustStockService;
import application.domain.services.inventory.ConsultInventoryService;
import application.domain.services.inventory.IssueForSaleService;
import application.domain.services.inventory.MarkDamagedService;
import application.domain.services.inventory.OpenStockRecordService;
import application.domain.services.inventory.ReceiveStockService;
import application.domain.services.inventory.ReleaseReservationService;
import application.domain.services.inventory.ReserveStockService;
import application.domain.services.inventory.RestockReturnService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the inventory domain services as beans.
 */
@Configuration
public class InventoryServicesConfig {

    @Bean
    public OpenStockRecordService openStockRecordService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateSellerOwnershipService validateSellerOwnershipService) {
        return new OpenStockRecordService(inventoryItemRepositoryPort, validateRoleAuthorizationService,
                validateSellerOwnershipService);
    }

    @Bean
    public ReceiveStockService receiveStockService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ReceiveStockService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public ReserveStockService reserveStockService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ReserveStockService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public ReleaseReservationService releaseReservationService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ReleaseReservationService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public IssueForSaleService issueForSaleService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new IssueForSaleService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public AdjustStockService adjustStockService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            RegisterOperationLogService registerOperationLogService) {
        return new AdjustStockService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService, registerOperationLogService);
    }

    @Bean
    public MarkDamagedService markDamagedService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new MarkDamagedService(inventoryItemRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public RestockReturnService restockReturnService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new RestockReturnService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public ConsultInventoryService consultInventoryService(
            InventoryItemRepositoryPort inventoryItemRepositoryPort,
            InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConsultInventoryService(inventoryItemRepositoryPort, inventoryMovementRepositoryPort,
                validateRoleAuthorizationService);
    }
}
