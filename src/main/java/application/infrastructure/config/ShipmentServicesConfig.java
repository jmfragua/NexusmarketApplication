package application.infrastructure.config;

import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.shipment.ConfirmShipmentDeliveryService;
import application.domain.services.shipment.ConsultShipmentService;
import application.domain.services.shipment.DispatchShipmentService;
import application.domain.services.shipment.PackShipmentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the logistics domain services as beans.
 */
@Configuration
public class ShipmentServicesConfig {

    @Bean
    public ConsultShipmentService consultShipmentService(
            ShipmentRepositoryPort shipmentRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService,
            ValidateBuyerOwnershipService validateBuyerOwnershipService) {
        return new ConsultShipmentService(shipmentRepositoryPort, validateRoleAuthorizationService,
                validateBuyerOwnershipService);
    }

    @Bean
    public PackShipmentService packShipmentService(
            ShipmentRepositoryPort shipmentRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new PackShipmentService(shipmentRepositoryPort, validateRoleAuthorizationService);
    }

    @Bean
    public DispatchShipmentService dispatchShipmentService(
            ShipmentRepositoryPort shipmentRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new DispatchShipmentService(shipmentRepositoryPort, orderRepositoryPort,
                validateRoleAuthorizationService);
    }

    @Bean
    public ConfirmShipmentDeliveryService confirmShipmentDeliveryService(
            ShipmentRepositoryPort shipmentRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        return new ConfirmShipmentDeliveryService(shipmentRepositoryPort, orderRepositoryPort,
                validateRoleAuthorizationService);
    }
}
