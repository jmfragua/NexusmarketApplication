package application.domain.services.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exercises the order lifecycle: a strictly sequential cycle that admits no jumps and no steps
 * backwards (RD-PED-01), ending in a terminal state that can never be modified (RD-PED-02).
 */
class OrderServicesTest {

    private ServiceHarness harness;

    private Buyer buyer;

    private User<UserId> operator;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        buyer = DomainFixtures.buyer();
        operator = DomainFixtures.logisticsOperator();
    }

    @Nested
    @DisplayName("Confirm payment")
    class ConfirmPayment {

        @Test
        @DisplayName("moves a pending order to paid and audits the transition")
        void movesToPaidAndAudits() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);

            Order result = harness.confirmPayment().confirmPayment(buyer, order);

            assertEquals(OrderStatus.PAID, result.getStatus());
            assertEquals(1, harness.operationLogs.size());
            assertEquals(OperationType.ORDER_STATUS_CHANGED,
                    harness.operationLogs.findAll().get(0).getOperationType());
        }

        @Test
        @DisplayName("is rejected for an order that belongs to another buyer (RD-PED-05)")
        void rejectsForeignOrder() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);
            Buyer intruder = DomainFixtures.buyer("buyer-2", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);

            assertThrows(UnauthorizedOperationException.class,
                    () -> harness.confirmPayment().confirmPayment(intruder, order));
            assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
            assertEquals(0, harness.operationLogs.size(), "a rejected operation is never audited");
        }

        @Test
        @DisplayName("cannot be applied twice: the cycle admits no repetition")
        void rejectsSecondPayment() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);

            assertThrows(InvalidOrderTransitionException.class,
                    () -> harness.confirmPayment().confirmPayment(buyer, order));
        }
    }

    @Nested
    @DisplayName("Dispatch order")
    class Dispatch {

        @Test
        @DisplayName("moves a paid order to dispatched")
        void movesToDispatched() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);

            assertEquals(OrderStatus.DISPATCHED,
                    harness.dispatchOrder().dispatch(operator, order).getStatus());
        }

        @Test
        @DisplayName("cannot skip the payment: the cycle is sequential (RD-PED-01)")
        void rejectsSkippingPayment() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);

            assertThrows(InvalidOrderTransitionException.class,
                    () -> harness.dispatchOrder().dispatch(operator, order));
        }

        @Test
        @DisplayName("is reserved to the logistics operator")
        void rejectsBuyer() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);

            assertThrows(UnauthorizedOperationException.class,
                    () -> harness.dispatchOrder().dispatch(buyer, order));
        }
    }

    @Nested
    @DisplayName("Complete delivery")
    class CompleteDelivery {

        @Test
        @DisplayName("closes the order at delivered, which is terminal")
        void closesTheOrder() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DISPATCHED);

            Order result = harness.completeDelivery().completeDelivery(operator, order);

            assertEquals(OrderStatus.DELIVERED, result.getStatus());
            assertFalse(result.isModifiable(), "a finished order can never be modified (RD-PED-02)");
        }

        @Test
        @DisplayName("a delivered order admits no further transition")
        void rejectsAnyTransitionAfterDelivery() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);

            assertThrows(OrderNotModifiableException.class,
                    () -> harness.completeDelivery().completeDelivery(operator, order));
            assertThrows(OrderNotModifiableException.class,
                    () -> harness.dispatchOrder().dispatch(operator, order));
            assertThrows(OrderNotModifiableException.class,
                    () -> harness.confirmPayment().confirmPayment(buyer, order));
        }

        @Test
        @DisplayName("cannot be reached without dispatching first")
        void rejectsSkippingDispatch() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);

            assertThrows(InvalidOrderTransitionException.class,
                    () -> harness.completeDelivery().completeDelivery(operator, order));
        }
    }

    @Nested
    @DisplayName("Consult order")
    class Consult {

        @Test
        @DisplayName("a buyer reaches their own order")
        void reachesOwnOrder() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);
            harness.orders().save(order);

            assertEquals(order, harness.consultOrder().consultOwn(buyer, OrderId.of("order-1")));
        }

        @Test
        @DisplayName("a buyer never reaches the order of another buyer (RD-ROL-04)")
        void neverReachesForeignOrder() {
            harness.orders().save(DomainFixtures.orderIn(buyer, OrderStatus.PAID));
            Buyer intruder = DomainFixtures.buyer("buyer-2", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);

            assertThrows(UnauthorizedOperationException.class,
                    () -> harness.consultOrder().consultOwn(intruder, OrderId.of("order-1")));
        }

        @Test
        @DisplayName("the supervisor consults by status as a monitoring profile")
        void supervisorConsultsByStatus() {
            harness.orders().save(DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED));

            assertEquals(1, harness.consultOrder()
                    .consultByStatus(DomainFixtures.supervisor(), OrderStatus.DELIVERED).size());
        }
    }
}
