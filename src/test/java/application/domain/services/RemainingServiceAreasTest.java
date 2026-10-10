package application.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.exceptions.ShipmentNotAllowedException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.exceptions.UserNotActiveException;
import application.domain.exceptions.WarehouseNotOwnedException;
import application.domain.models.Buyer;
import application.domain.models.DigitalProduct;
import application.domain.models.Invoice;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.services.invoice.ConsultInvoiceService;
import application.domain.services.invoice.IssueInvoiceService;
import application.domain.services.reporting.ConsultInventoryReportService;
import application.domain.services.reporting.ConsultSalesReportService;
import application.domain.services.seller.ConsultSellerService;
import application.domain.services.seller.RegisterSellerService;
import application.domain.services.shipment.ConfirmShipmentDeliveryService;
import application.domain.services.shipment.DispatchShipmentService;
import application.domain.services.shipment.PackShipmentService;
import application.domain.services.user.ActivateUserService;
import application.domain.services.user.BlockUserService;
import application.domain.services.user.ChangeUserRoleService;
import application.domain.services.user.LoginService;
import application.domain.services.user.RegisterBuyerService;
import application.domain.services.warehouse.ConsultWarehouseService;
import application.domain.services.warehouse.RegisterMarketplaceWarehouseService;
import application.domain.support.DomainFixtures;
import application.domain.support.ServiceHarness;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.StockCondition;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the service areas that only had indirect coverage: user, seller, warehouse, invoicing,
 * logistics and reporting.
 *
 * <p>Each area is exercised through its happy path and through the rule that most matters in it —
 * the one that would do real damage if it stopped holding.</p>
 */
class RemainingServiceAreasTest {

    private ServiceHarness harness;

    private User<UserId> administrator;

    private User<UserId> operator;

    @BeforeEach
    void setUp() {
        harness = new ServiceHarness();
        administrator = DomainFixtures.administrator();
        operator = DomainFixtures.logisticsOperator();
    }

    private RegisterBuyerService registerBuyer() {
        return new RegisterBuyerService(harness.users, harness.carts());
    }

    private RegisterSellerService registerSeller() {
        return new RegisterSellerService(harness.sellers, harness.warehouses, harness.users,
                harness.validateRole, harness.registerOperationLog);
    }

    @Nested
    @DisplayName("User")
    class UserArea {

        @Test
        @DisplayName("registering a buyer stores them along with their active cart")
        void registersBuyerWithCart() {
            Buyer buyer = DomainFixtures.buyer();

            registerBuyer().register(buyer, CartId.of("cart-1"));

            assertTrue(harness.users.findByEmail(buyer.getEmail()).isPresent());
            assertTrue(harness.carts().findByBuyer(buyer.getIdentifier()).isPresent());
        }

        @Test
        @DisplayName("a duplicated email is rejected and nothing is stored (RD-ID-03)")
        void rejectsDuplicatedEmail() {
            registerBuyer().register(DomainFixtures.buyer(), CartId.of("cart-1"));
            Buyer twin = new Buyer(application.domain.valuesObjects.BuyerId.of("buyer-2"),
                    FullName.of("Otra"), EmailAddress.of("buyer-1@nexusmarket.test"),
                    IdentityDocument.of("doc-distinto"), UserStatus.ACTIVE,
                    Address.of("Otra calle"), BuyerCommercialStatus.ENABLED);

            assertThrows(DuplicateEmailException.class,
                    () -> registerBuyer().register(twin, CartId.of("cart-2")));
            assertEquals(1, harness.users.findAll().size());
        }

        @Test
        @DisplayName("a duplicated identity document is rejected (RD-ID-04)")
        void rejectsDuplicatedDocument() {
            registerBuyer().register(DomainFixtures.buyer(), CartId.of("cart-1"));
            Buyer twin = new Buyer(application.domain.valuesObjects.BuyerId.of("buyer-3"),
                    FullName.of("Otra"), EmailAddress.of("distinto@nexusmarket.test"),
                    IdentityDocument.of("doc-buyer-1"), UserStatus.ACTIVE,
                    Address.of("Otra calle"), BuyerCommercialStatus.ENABLED);

            assertThrows(DuplicateIdentityDocumentException.class,
                    () -> registerBuyer().register(twin, CartId.of("cart-3")));
        }

        @Test
        @DisplayName("blocking and reactivating a user is reserved to the administrator")
        void blockAndActivateAreAdministrative() {
            Buyer buyer = DomainFixtures.buyer();
            BlockUserService block = new BlockUserService(harness.users, harness.validateRole);
            ActivateUserService activate = new ActivateUserService(harness.users, harness.validateRole);

            block.block(administrator, buyer);
            assertEquals(UserStatus.BLOCKED, buyer.getStatus());

            activate.activate(administrator, buyer);
            assertEquals(UserStatus.ACTIVE, buyer.getStatus());

            assertThrows(UnauthorizedOperationException.class,
                    () -> block.block(DomainFixtures.supervisor(), buyer));
        }

        @Test
        @DisplayName("the role stays unique per user: reassigning replaces it (RG-02)")
        void roleIsReplacedNotAccumulated() {
            User<UserId> staff = DomainFixtures.userWithRole("staff-1", UserRole.SUPERVISOR);
            ChangeUserRoleService change = new ChangeUserRoleService(harness.users, harness.validateRole);

            change.changeRole(administrator, staff, UserRole.LOGISTICS_OPERATOR);

            assertEquals(UserRole.LOGISTICS_OPERATOR, staff.getRole());
        }

        @Test
        @DisplayName("login resolves the user and refuses a blocked one (RG-01)")
        void loginRefusesBlockedUser() {
            Buyer buyer = DomainFixtures.buyer();
            harness.users.save(buyer);
            LoginService login = new LoginService(harness.users, harness.validateUserActiveStatus);

            assertEquals(buyer, login.login(buyer.getEmail()));

            buyer.block();
            assertThrows(UserNotActiveException.class, () -> login.login(buyer.getEmail()));
            assertThrows(EntityNotFoundException.class,
                    () -> login.login(EmailAddress.of("nadie@nexusmarket.test")));
        }
    }

    @Nested
    @DisplayName("Seller")
    class SellerArea {

        @Test
        @DisplayName("incorporation stores the seller and their first warehouse together (RD-ROL-05)")
        void incorporatesSellerWithWarehouse() {
            SellerId sellerId = SellerId.of("seller-new");
            SellerWarehouse warehouse = new SellerWarehouse(WarehouseId.of("wh-new"), sellerId);

            registerSeller().register(administrator, sellerId, FullName.of("Carlos"),
                    EmailAddress.of("carlos@nexusmarket.test"), IdentityDocument.of("doc-carlos"),
                    UserStatus.ACTIVE, warehouse);

            assertTrue(harness.sellers.findById(sellerId).isPresent());
            assertEquals(1, harness.warehouses.findBySeller(sellerId).size());
        }

        @Test
        @DisplayName("only the administrator may incorporate a seller")
        void onlyAdministratorIncorporates() {
            SellerId sellerId = SellerId.of("seller-new");
            SellerWarehouse warehouse = new SellerWarehouse(WarehouseId.of("wh-new"), sellerId);

            assertThrows(UnauthorizedOperationException.class, () -> registerSeller()
                    .register(operator, sellerId, FullName.of("Carlos"),
                            EmailAddress.of("carlos@nexusmarket.test"), IdentityDocument.of("doc"),
                            UserStatus.ACTIVE, warehouse));
        }

        @Test
        @DisplayName("a warehouse belonging to another seller is rejected")
        void rejectsForeignFirstWarehouse() {
            SellerWarehouse foreign = new SellerWarehouse(WarehouseId.of("wh-x"), SellerId.of("otro"));

            assertThrows(IllegalArgumentException.class, () -> registerSeller()
                    .register(administrator, SellerId.of("seller-new"), FullName.of("Carlos"),
                            EmailAddress.of("carlos@nexusmarket.test"), IdentityDocument.of("doc"),
                            UserStatus.ACTIVE, foreign));
        }

        @Test
        @DisplayName("consultation is limited to administrator and supervisor")
        void consultationIsLimited() {
            ConsultSellerService consult = new ConsultSellerService(harness.sellers, harness.validateRole);
            harness.sellers.save(DomainFixtures.seller());

            assertEquals(1, consult.consultAll(administrator).size());
            assertEquals(1, consult.consultAll(DomainFixtures.supervisor()).size());
            assertThrows(UnauthorizedOperationException.class, () -> consult.consultAll(operator));
            assertThrows(EntityNotFoundException.class,
                    () -> consult.consult(administrator, SellerId.of("no-existe")));
        }
    }

    @Nested
    @DisplayName("Warehouse")
    class WarehouseArea {

        private RegisterMarketplaceWarehouseService register() {
            return new RegisterMarketplaceWarehouseService(harness.warehouses, harness.validateRole);
        }

        private ConsultWarehouseService consult() {
            return new ConsultWarehouseService(harness.warehouses, harness.validateRole,
                    harness.validateSellerOwnership);
        }

        @Test
        @DisplayName("the administrator registers a marketplace warehouse, with no owner")
        void registersMarketplaceWarehouse() {
            MarketplaceWarehouse warehouse = register().register(administrator, WarehouseId.of("wh-mk"));

            assertEquals(application.domain.valuesObjects.WarehouseType.MARKETPLACE, warehouse.getType());
            assertTrue(harness.warehouses.findById(WarehouseId.of("wh-mk")).isPresent());
        }

        @Test
        @DisplayName("a seller reaches their own warehouse but never another one (RG-03)")
        void sellerOnlyReachesOwnWarehouse() {
            Seller seller = DomainFixtures.seller();
            harness.warehouses.save(DomainFixtures.sellerWarehouse(seller));
            harness.warehouses.save(new MarketplaceWarehouse(WarehouseId.of("wh-mk")));

            assertEquals(1, consult().consultOwn(seller).size());
            assertThrows(WarehouseNotOwnedException.class,
                    () -> consult().consultOwn(seller, WarehouseId.of("wh-mk")));
        }

        @Test
        @DisplayName("the logistics operator reaches every warehouse")
        void operatorReachesAll() {
            harness.warehouses.save(new MarketplaceWarehouse(WarehouseId.of("wh-mk")));
            harness.warehouses.save(DomainFixtures.sellerWarehouse(DomainFixtures.seller()));

            assertEquals(2, consult().consultAll(operator).size());
        }
    }

    @Nested
    @DisplayName("Invoicing")
    class InvoiceArea {

        private IssueInvoiceService issue() {
            return new IssueInvoiceService(harness.commerce.invoices(), harness.validateRole);
        }

        @Test
        @DisplayName("an invoice is issued from a paid order and mirrors its total (RD-FAC-01)")
        void issuesFromPaidOrder() {
            Order order = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PAID);

            Invoice invoice = issue().issue(administrator, order, InvoiceId.of("inv-1"));

            assertEquals(order.getTotalAmount(), invoice.getTotalAmount());
            assertTrue(invoice.matchesOrderTotal(order));
        }

        @Test
        @DisplayName("an unpaid order cannot be invoiced")
        void refusesUnpaidOrder() {
            Order pending = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PENDING_PAYMENT);

            assertThrows(InvalidOrderTransitionException.class,
                    () -> issue().issue(administrator, pending, InvoiceId.of("inv-1")));
        }

        @Test
        @DisplayName("a buyer reaches their own invoice but not one of another buyer")
        void buyerOnlyReachesOwnInvoice() {
            Buyer buyer = DomainFixtures.buyer();
            Order order = DomainFixtures.orderIn(buyer, OrderStatus.PAID);
            issue().issue(administrator, order, InvoiceId.of("inv-1"));
            ConsultInvoiceService consult = new ConsultInvoiceService(harness.commerce.invoices(),
                    harness.validateRole, harness.validateBuyerOwnership);

            assertEquals(1, consult.consultOwn(buyer).size());

            Buyer intruder = DomainFixtures.buyer("buyer-x", UserStatus.ACTIVE, BuyerCommercialStatus.ENABLED);
            assertThrows(UnauthorizedOperationException.class, () -> consult.consultOwn(intruder, order));
        }
    }

    @Nested
    @DisplayName("Logistics")
    class ShipmentArea {

        private Shipment shipmentFor(Order order) {
            return new Shipment(ShipmentId.of("ship-1"), order.getIdentifier(),
                    WarehouseId.of("warehouse-1"), order.getDeliveryAddress());
        }

        private List<Product> physicalProducts() {
            return List.of(DomainFixtures.sellableProduct(DomainFixtures.seller()));
        }

        @Test
        @DisplayName("packing requires a paid order (RD-PED-03)")
        void packingRequiresPaidOrder() {
            Order paid = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PAID);
            PackShipmentService pack = new PackShipmentService(harness.shipments(), harness.validateRole);

            pack.pack(operator, shipmentFor(paid), paid, physicalProducts());

            Order pending = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PENDING_PAYMENT);
            assertThrows(IllegalStateException.class,
                    () -> pack.pack(operator, shipmentFor(pending), pending, physicalProducts()));
        }

        /**
         * Digital products are delivered the moment the payment is confirmed and never generate a
         * shipment (RD-LOG-01).
         */
        @Test
        @DisplayName("an order with only digital products generates no shipment")
        void digitalOrderGeneratesNoShipment() {
            Order order = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PAID);
            List<Product> digital = List.of(new DigitalProduct(ProductId.of("product-1"),
                    DomainFixtures.seller().getIdentifier()));
            PackShipmentService pack = new PackShipmentService(harness.shipments(), harness.validateRole);

            assertThrows(ShipmentNotAllowedException.class,
                    () -> pack.pack(operator, shipmentFor(order), order, digital));
        }

        @Test
        @DisplayName("dispatch and delivery move the order and are saved together")
        void dispatchAndDeliveryMoveTheOrder() {
            Order order = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.PAID);
            Shipment shipment = shipmentFor(order);
            DispatchShipmentService dispatch = new DispatchShipmentService(harness.shipments(),
                    harness.orders(), harness.validateRole);
            ConfirmShipmentDeliveryService deliver = new ConfirmShipmentDeliveryService(harness.shipments(),
                    harness.orders(), harness.validateRole);

            dispatch.dispatch(operator, shipment, order);
            assertEquals(OrderStatus.DISPATCHED, order.getStatus());

            deliver.confirmDelivery(operator, shipment, order);
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
            assertTrue(harness.shipments().findByOrder(order.getIdentifier()).size() >= 1);
        }

        @Test
        @DisplayName("a delivered order admits no further logistics (RD-PED-02)")
        void deliveredOrderIsClosed() {
            Order delivered = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.DELIVERED);
            DispatchShipmentService dispatch = new DispatchShipmentService(harness.shipments(),
                    harness.orders(), harness.validateRole);

            assertThrows(OrderNotModifiableException.class,
                    () -> dispatch.dispatch(operator, shipmentFor(delivered), delivered));
        }
    }

    @Nested
    @DisplayName("Reporting")
    class ReportingArea {

        private ConsultSalesReportService sales() {
            return new ConsultSalesReportService(harness.orders(), harness.commerce.invoices(),
                    harness.validateRole);
        }

        private ConsultInventoryReportService inventory() {
            return new ConsultInventoryReportService(harness.inventoryItems(),
                    harness.inventory.movements(), harness.validateRole);
        }

        @Test
        @DisplayName("sales reporting lists completed orders and adds up what was invoiced")
        void addsUpInvoicedTotal() {
            Order delivered = DomainFixtures.orderIn(DomainFixtures.buyer(), OrderStatus.DELIVERED);
            harness.orders().save(delivered);
            harness.commerce.invoices().save(new Invoice(InvoiceId.of("inv-1"), delivered.getIdentifier(),
                    delivered.getBuyerId(), DomainFixtures.money(20000)));

            assertEquals(1, sales().consultCompletedOrders(administrator).size());
            assertEquals(DomainFixtures.money(20000),
                    sales().consultInvoicedTotal(administrator).orElseThrow());
        }

        /**
         * There is no neutral {@code Money} without a currency, so an empty total is genuinely empty
         * rather than a zero in some arbitrary currency (RD-ALC-03).
         */
        @Test
        @DisplayName("with nothing invoiced the total is empty, not zero")
        void emptyTotalWhenNothingInvoiced() {
            assertTrue(sales().consultInvoicedTotal(administrator).isEmpty());
        }

        @Test
        @DisplayName("inventory reporting isolates damaged stock (RD-INV-03)")
        void isolatesDamagedStock() {
            harness.inventoryItems().save(DomainFixtures.stockWith(5, 0, StockCondition.DAMAGED));

            assertEquals(1, inventory().consultDamagedStock(administrator).size());
            assertEquals(1, inventory().consultStockByWarehouse(administrator,
                    WarehouseId.of("warehouse-1")).size());
        }

        @Test
        @DisplayName("reporting is denied to roles that only operate")
        void deniedToOperationalRoles() {
            assertThrows(UnauthorizedOperationException.class,
                    () -> sales().consultCompletedOrders(operator));
            assertThrows(UnauthorizedOperationException.class,
                    () -> inventory().consultDamagedStock(DomainFixtures.buyer()));
        }
    }
}
