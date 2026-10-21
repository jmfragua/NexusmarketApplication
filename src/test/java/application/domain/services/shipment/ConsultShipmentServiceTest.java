package application.domain.services.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.WarehouseId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Shipments are read by the operation roles for any order, and by a buyer only for their own orders
 * (RD-PED-05, RD-ROL-04).
 */
class ConsultShipmentServiceTest {

    private ConsultShipmentService service;

    private ShipmentRepositoryPort shipments;

    private Buyer buyer;

    private Order order;

    @BeforeEach
    void setUp() {
        ServiceHarness harness = new ServiceHarness();
        shipments = harness.commerce.shipments();
        service = new ConsultShipmentService(shipments, harness.validateRole, harness.validateBuyerOwnership);
        buyer = DomainFixtures.buyer();
        order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);
    }

    @Test
    @DisplayName("is empty until the order is packed, then returns its shipment")
    void emptyUntilPacked() {
        assertTrue(service.consultByOrder(DomainFixtures.logisticsOperator(), order).isEmpty());

        shipments.save(new Shipment(ShipmentId.of("shp-1"), order.getIdentifier(), WarehouseId.of("warehouse-1"),
                order.getDeliveryAddress()));

        assertEquals(1, service.consultByOrder(DomainFixtures.logisticsOperator(), order).size());
        assertEquals(1, service.consultOwn(buyer, order).size());
    }

    @Test
    @DisplayName("a buyer cannot read the shipments of another buyer's order")
    void buyerReadsOnlyOwnOrders() {
        Buyer other = DomainFixtures.buyer("buyer-2", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);

        assertThrows(UnauthorizedOperationException.class, () -> service.consultOwn(other, order));
    }

    @Test
    @DisplayName("sellers and buyers do not use the operational query")
    void operationalQueryIsForOperationRoles() {
        assertThrows(UnauthorizedOperationException.class, () -> service.consultByOrder(DomainFixtures.seller(), order));
        assertThrows(UnauthorizedOperationException.class, () -> service.consultByOrder(buyer, order));
    }
}
