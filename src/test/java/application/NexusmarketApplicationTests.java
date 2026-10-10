package application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import application.domain.ports.in.AdministratorPort;
import application.domain.ports.in.BuyerPort;
import application.domain.ports.in.LogisticOperatorPort;
import application.domain.ports.in.PublicAccessPort;
import application.domain.ports.in.SellerPort;
import application.domain.ports.in.SupervisorPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OperationLogRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the whole dependency graph assembles: the adapters satisfy every output port, the
 * configuration classes expose every domain service, and the use cases satisfy every input port.
 *
 * <p>This is the checkpoint the hexagonal wiring needs — a compiling project proves nothing about
 * whether Spring can actually resolve the graph.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class NexusmarketApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("the application context loads")
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    @DisplayName("every one of the 49 domain services is exposed as a bean")
    void everyDomainServiceIsABean() {
        long serviceBeans = applicationContext.getBeansOfType(Object.class).values().stream()
                .map(bean -> bean.getClass().getName())
                .filter(name -> name.startsWith("application.domain.services."))
                .count();
        assertEquals(49, serviceBeans, "every domain service must be wired as a bean");
    }

    @Test
    @DisplayName("every output port has exactly one adapter")
    void everyOutputPortHasAnAdapter() {
        assertNotNull(applicationContext.getBean(UserRepositoryPort.class));
        assertNotNull(applicationContext.getBean(SellerRepositoryPort.class));
        assertNotNull(applicationContext.getBean(WarehouseRepositoryPort.class));
        assertNotNull(applicationContext.getBean(ProductRepositoryPort.class));
        assertNotNull(applicationContext.getBean(InventoryItemRepositoryPort.class));
        assertNotNull(applicationContext.getBean(InventoryMovementRepositoryPort.class));
        assertNotNull(applicationContext.getBean(CartRepositoryPort.class));
        assertNotNull(applicationContext.getBean(OrderRepositoryPort.class));
        assertNotNull(applicationContext.getBean(InvoiceRepositoryPort.class));
        assertNotNull(applicationContext.getBean(ShipmentRepositoryPort.class));
        assertNotNull(applicationContext.getBean(ProductReturnRepositoryPort.class));
        assertNotNull(applicationContext.getBean(RefundRepositoryPort.class));
        assertNotNull(applicationContext.getBean(OperationLogRepositoryPort.class));
    }

    @Test
    @DisplayName("every input port has exactly one use case implementation")
    void everyInputPortHasAUseCase() {
        assertNotNull(applicationContext.getBean(PublicAccessPort.class));
        assertNotNull(applicationContext.getBean(BuyerPort.class));
        assertNotNull(applicationContext.getBean(SellerPort.class));
        assertNotNull(applicationContext.getBean(LogisticOperatorPort.class));
        assertNotNull(applicationContext.getBean(AdministratorPort.class));
        assertNotNull(applicationContext.getBean(SupervisorPort.class));
    }
}
