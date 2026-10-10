package application.domain.services.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.RefundExceedsInvoiceException;
import application.domain.exceptions.ReturnNotAllowedException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Invoice;
import application.domain.models.OperationLog;
import application.domain.models.Order;
import application.domain.models.ProductReturn;
import application.domain.models.User;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Exercises the after sales cycle — returns and refunds (RD-POS-01, RD-POS-02) — together with the
 * audit trail that the refund and the order transitions leave behind (RD-AUD-01).
 */
class AuditAndAfterSalesServicesTest {

    private ServiceHarness harness;

    private Buyer buyer;

    private User<UserId> administrator;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        buyer = DomainFixtures.buyer();
        administrator = DomainFixtures.administrator();
    }

    private Invoice invoiceOf(Order order) {
        return new Invoice(InvoiceId.of("invoice-1"), order.getIdentifier(), buyer.getIdentifier(),
                order.getTotalAmount());
    }

    @Nested
    @DisplayName("Request return")
    class RequestReturn {

        @Test
        @DisplayName("registers a return over a delivered order")
        void registersOverDeliveredOrder() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);

            ProductReturn result = harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(1), ProductReturnId.of("return-1"));

            assertEquals(1, result.getQuantity().getValue());
            assertTrue(result.isFor(order));
        }

        @Test
        @DisplayName("is rejected while the order has not been delivered (RD-POS-01)")
        void rejectsUndeliveredOrder() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DISPATCHED);

            assertThrows(ReturnNotAllowedException.class, () -> harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(1), ProductReturnId.of("return-1")));
        }

        @Test
        @DisplayName("is rejected when the quantity exceeds the one of the line")
        void rejectsExcessiveQuantity() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);

            assertThrows(IllegalArgumentException.class, () -> harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(99), ProductReturnId.of("return-1")));
        }
    }

    @Nested
    @DisplayName("Issue refund")
    class IssueRefund {

        @Test
        @DisplayName("issues the refund and audits it, the only outflow of money in the system")
        void issuesAndAudits() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);
            ProductReturn productReturn = harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(1), ProductReturnId.of("return-1"));

            harness.issueRefund().issue(administrator, productReturn, invoiceOf(order),
                    DomainFixtures.money(10000), RefundId.of("refund-1"));

            assertEquals(1, harness.operationLogs.size());
            OperationLog log = harness.operationLogs.findAll().get(0);
            assertEquals(OperationType.REFUND_ISSUED, log.getOperationType());
            assertEquals(administrator.getIdentifier(), log.getActorId());
        }

        @Test
        @DisplayName("is rejected when the amount exceeds the invoiced total (RD-POS-02)")
        void rejectsAmountOverInvoice() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);
            ProductReturn productReturn = harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(1), ProductReturnId.of("return-1"));

            assertThrows(RefundExceedsInvoiceException.class, () -> harness.issueRefund().issue(
                    administrator, productReturn, invoiceOf(order),
                    DomainFixtures.money(999999), RefundId.of("refund-1")));
            assertEquals(0, harness.operationLogs.size(), "a rejected refund leaves no audit entry");
        }

        @Test
        @DisplayName("is reserved to the administrator, who manages refunds (RD-ROL-06)")
        void rejectsNonAdministrator() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.DELIVERED);
            ProductReturn productReturn = harness.requestReturn().requestReturn(
                    buyer, order, order.getLines().get(0), Quantity.of(1), ProductReturnId.of("return-1"));

            assertThrows(UnauthorizedOperationException.class, () -> harness.issueRefund().issue(
                    DomainFixtures.logisticsOperator(), productReturn, invoiceOf(order),
                    DomainFixtures.money(10000), RefundId.of("refund-1")));
        }
    }

    @Nested
    @DisplayName("Audit trail")
    class AuditTrail {

        @Test
        @DisplayName("records the actor and the affected entity of each logged operation")
        void recordsActorAndEntity() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);

            harness.confirmPayment().confirmPayment(buyer, order);

            OperationLog log = harness.operationLogs.findAll().get(0);
            assertEquals(buyer.getIdentifier(), log.getActorId());
            assertEquals(buyer.getRole(), log.getActorRole());
            assertEquals("Order", log.getAffectedEntityType());
            assertEquals(order.getIdentifier().getValue(), log.getAffectedEntityId());
        }

        @Test
        @DisplayName("the whole order lifecycle leaves one entry per transition")
        void oneEntryPerTransition() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);
            User<UserId> operator = DomainFixtures.logisticsOperator();

            harness.confirmPayment().confirmPayment(buyer, order);
            harness.dispatchOrder().dispatch(operator, order);
            harness.completeDelivery().completeDelivery(operator, order);

            assertEquals(3, harness.operationLogs.size());
            assertEquals(3, harness.operationLogs
                    .findByOperationType(OperationType.ORDER_STATUS_CHANGED).size());
        }

        @Test
        @DisplayName("operations outside the agreed set are not audited")
        void doesNotAuditEverything() {
            harness.receiveStock().receive(DomainFixtures.logisticsOperator(),
                    DomainFixtures.emptyStock(), Quantity.of(5),
                    application.domain.valuesObjects.InventoryMovementId.of("mov-1"));

            assertEquals(0, harness.operationLogs.size(),
                    "receiving stock is traced by its own movement, not by the audit log");
        }

        @Test
        @DisplayName("the log can be queried by actor, which demands the right identifier subtype")
        void queryableByActor() {
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PENDING_PAYMENT);
            harness.confirmPayment().confirmPayment(buyer, order);

            assertEquals(1, harness.operationLogs.findByActor(buyer.getIdentifier()).size());
            assertTrue(harness.operationLogs.findByActor(administrator.getIdentifier()).isEmpty());
        }
    }
}
