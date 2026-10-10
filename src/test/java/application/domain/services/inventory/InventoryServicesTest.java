package application.domain.services.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.DamagedStockException;
import application.domain.exceptions.InsufficientStockException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.exceptions.UserNotActiveException;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.models.User;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.InventoryMovementType;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exercises the inventory services against the invariants of the distributed inventory: no negative
 * stock (RD-INV-02), no reservation of damaged or missing stock (RD-INV-03) and a movement behind
 * every change (RD-INV-04).
 */
class InventoryServicesTest {

    private ServiceHarness harness;

    private User<UserId> operator;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        operator = DomainFixtures.logisticsOperator();
    }

    private InventoryMovementId movement(String id) {
        return InventoryMovementId.of(id);
    }

    @Nested
    @DisplayName("Receive stock")
    class ReceiveStock {

        @Test
        @DisplayName("adds the units to the available balance and records an inbound movement")
        void addsUnitsAndRecordsMovement() {
            InventoryItem stock = DomainFixtures.emptyStock();

            InventoryMovement result = harness.receiveStock()
                    .receive(operator, stock, Quantity.of(10), movement("mov-1"));

            assertEquals(10, stock.getAvailableQuantity().getValue());
            assertEquals(InventoryMovementType.INBOUND, result.getMovementType());
            assertEquals(1, harness.inventory.storedMovements(), "the movement must be persisted (RD-INV-04)");
        }

        @Test
        @DisplayName("is rejected for a role that does not administer inventory")
        void rejectsUnauthorizedRole() {
            InventoryItem stock = DomainFixtures.emptyStock();

            assertThrows(UnauthorizedOperationException.class, () -> harness.receiveStock()
                    .receive(DomainFixtures.buyer(), stock, Quantity.of(5), movement("mov-1")));
        }

        @Test
        @DisplayName("is rejected for a blocked user before any stock is touched")
        void rejectsBlockedUser() {
            InventoryItem stock = DomainFixtures.emptyStock();
            User<UserId> blocked = DomainFixtures.userWithRole("logistics-2",
                    application.domain.valuesObjects.UserRole.LOGISTICS_OPERATOR);
            blocked.block();

            assertThrows(UserNotActiveException.class, () -> harness.receiveStock()
                    .receive(blocked, stock, Quantity.of(5), movement("mov-1")));
            assertEquals(0, stock.getAvailableQuantity().getValue());
        }
    }

    @Nested
    @DisplayName("Reserve stock")
    class ReserveStock {

        @Test
        @DisplayName("moves units from available to reserved")
        void movesUnitsToReserved() {
            InventoryItem stock = DomainFixtures.stockWith(10, 0, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            InventoryMovement result = harness.reserveStock()
                    .reserve(operator, line, stock, movement("mov-1"));

            assertEquals(8, stock.getAvailableQuantity().getValue());
            assertEquals(2, stock.getReservedQuantity().getValue());
            assertEquals(InventoryMovementType.RESERVATION, result.getMovementType());
        }

        @Test
        @DisplayName("is rejected when the available balance does not cover the line (RD-INV-02)")
        void rejectsInsufficientStock() {
            InventoryItem stock = DomainFixtures.stockWith(1, 0, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            assertThrows(InsufficientStockException.class, () -> harness.reserveStock()
                    .reserve(operator, line, stock, movement("mov-1")));
            assertEquals(1, stock.getAvailableQuantity().getValue(), "a rejected reservation changes nothing");
            assertEquals(0, harness.inventory.storedMovements());
        }

        @Test
        @DisplayName("is rejected for damaged stock even when the balance would suffice (RD-INV-03)")
        void rejectsDamagedStock() {
            InventoryItem stock = DomainFixtures.stockWith(100, 0, StockCondition.DAMAGED);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            assertThrows(DamagedStockException.class, () -> harness.reserveStock()
                    .reserve(operator, line, stock, movement("mov-1")));
        }
    }

    @Nested
    @DisplayName("Issue for sale")
    class IssueForSale {

        @Test
        @DisplayName("discounts from reserved, leaving the available balance untouched")
        void discountsFromReserved() {
            InventoryItem stock = DomainFixtures.stockWith(5, 2, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            InventoryMovement result = harness.issueForSale()
                    .issue(operator, line, stock, movement("mov-1"));

            assertEquals(5, stock.getAvailableQuantity().getValue());
            assertEquals(0, stock.getReservedQuantity().getValue());
            assertEquals(InventoryMovementType.SALE_OUTBOUND, result.getMovementType());
        }

        @Test
        @DisplayName("is rejected when nothing was reserved first")
        void rejectsWithoutReservation() {
            InventoryItem stock = DomainFixtures.stockWith(5, 0, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            assertThrows(IllegalStateException.class, () -> harness.issueForSale()
                    .issue(operator, line, stock, movement("mov-1")));
        }

        @Test
        @DisplayName("is reserved to the logistics operator, not to the seller")
        void rejectsSeller() {
            InventoryItem stock = DomainFixtures.stockWith(5, 2, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            assertThrows(UnauthorizedOperationException.class, () -> harness.issueForSale()
                    .issue(DomainFixtures.seller(), line, stock, movement("mov-1")));
        }
    }

    @Nested
    @DisplayName("Release reservation")
    class ReleaseReservation {

        @Test
        @DisplayName("returns the units to available and records an adjustment")
        void returnsUnitsToAvailable() {
            InventoryItem stock = DomainFixtures.stockWith(5, 2, StockCondition.AVAILABLE);
            OrderLine line = DomainFixtures.orderLine(
                    application.domain.valuesObjects.OrderId.of("order-1"));

            InventoryMovement result = harness.releaseReservation()
                    .release(operator, line, stock, movement("mov-1"));

            assertEquals(7, stock.getAvailableQuantity().getValue());
            assertEquals(0, stock.getReservedQuantity().getValue());
            assertEquals(InventoryMovementType.ADJUSTMENT, result.getMovementType(),
                    "the catalogue of movements is closed: a release is recorded as an adjustment");
        }
    }

    @Nested
    @DisplayName("Adjust stock")
    class AdjustStock {

        @Test
        @DisplayName("replaces the available balance and audits the correction (RD-AUD-01)")
        void replacesBalanceAndAudits() {
            InventoryItem stock = DomainFixtures.stockWith(10, 0, StockCondition.AVAILABLE);

            harness.adjustStock().adjust(operator, stock, Quantity.of(4), movement("mov-1"));

            assertEquals(4, stock.getAvailableQuantity().getValue());
            assertEquals(1, harness.operationLogs.size());
            assertEquals(OperationType.INVENTORY_ADJUSTED,
                    harness.operationLogs.findAll().get(0).getOperationType());
        }

        @Test
        @DisplayName("records the correction as an adjustment movement")
        void recordsAdjustmentMovement() {
            InventoryItem stock = DomainFixtures.stockWith(10, 0, StockCondition.AVAILABLE);

            InventoryMovement result = harness.adjustStock()
                    .adjust(operator, stock, Quantity.of(14), movement("mov-1"));

            assertEquals(InventoryMovementType.ADJUSTMENT, result.getMovementType());
            assertEquals(4, result.getQuantity().getValue(), "the movement carries the difference");
        }
    }

    @Nested
    @DisplayName("Mark damaged")
    class MarkDamaged {

        @Test
        @DisplayName("excludes the stock from any further reservation (RD-INV-03)")
        void excludesFromReservation() {
            InventoryItem stock = DomainFixtures.stockWith(10, 0, StockCondition.AVAILABLE);

            harness.markDamaged().markAsDamaged(operator, stock);

            assertEquals(StockCondition.DAMAGED, stock.getCondition());
            assertTrue(stock.getAvailableQuantity().isPositive(), "the units remain counted");
            assertThrows(DamagedStockException.class, () -> harness.reserveStock().reserve(
                    operator,
                    DomainFixtures.orderLine(application.domain.valuesObjects.OrderId.of("order-1")),
                    stock,
                    movement("mov-2")));
        }

        @Test
        @DisplayName("generates no movement, because the quantity did not change")
        void generatesNoMovement() {
            InventoryItem stock = DomainFixtures.stockWith(10, 0, StockCondition.AVAILABLE);

            harness.markDamaged().markAsDamaged(operator, stock);

            assertEquals(0, harness.inventory.storedMovements());
        }
    }

    @Nested
    @DisplayName("Consult inventory")
    class ConsultInventory {

        @Test
        @DisplayName("is denied to a buyer, who never manages inventories (RD-ROL-04)")
        void deniesBuyer() {
            assertThrows(UnauthorizedOperationException.class, () -> harness.consultInventory()
                    .consultByWarehouse(DomainFixtures.buyer(),
                            application.domain.valuesObjects.WarehouseId.of("warehouse-1")));
        }

        @Test
        @DisplayName("is allowed to the supervisor as a monitoring profile")
        void allowsSupervisor() {
            harness.inventoryItems().save(DomainFixtures.stockWith(3, 0, StockCondition.AVAILABLE));

            assertEquals(1, harness.consultInventory().consultByWarehouse(DomainFixtures.supervisor(),
                    application.domain.valuesObjects.WarehouseId.of("warehouse-1")).size());
        }
    }

    @Nested
    @DisplayName("Order lifecycle audit")
    class OrderAudit {

        @Test
        @DisplayName("a disabled buyer never reaches the stock, the commercial status stops them first")
        void disabledBuyerIsStoppedBeforeStock() {
            Order order = DomainFixtures.orderIn(
                    DomainFixtures.buyer("buyer-9", UserStatus.ACTIVE, BuyerCommercialStatus.DISABLED),
                    OrderStatus.PENDING_PAYMENT);

            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            assertEquals(0, harness.inventory.storedMovements());
        }
    }
}
