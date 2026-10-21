package application.domain.support;

import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateBuyerOwnershipService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.authorization.ValidateSellerOwnershipService;
import application.domain.services.authorization.ValidateUserActiveStatusService;
import application.domain.services.cart.AddCartItemService;
import application.domain.services.cart.ClearCartService;
import application.domain.services.cart.ConfirmCartService;
import application.domain.services.cart.RemoveCartItemService;
import application.domain.services.cart.UpdateCartItemQuantityService;
import application.domain.services.catalog.AddProductVariantService;
import application.domain.services.catalog.ConsultCatalogService;
import application.domain.services.catalog.DiscontinueProductService;
import application.domain.services.catalog.PublishProductService;
import application.domain.services.catalog.RegisterProductService;
import application.domain.services.catalog.RemoveProductVariantService;
import application.domain.services.catalog.SuspendProductService;
import application.domain.services.inventory.AdjustStockService;
import application.domain.services.inventory.ConsultInventoryService;
import application.domain.services.inventory.IssueForSaleService;
import application.domain.services.inventory.MarkDamagedService;
import application.domain.services.inventory.OpenStockRecordService;
import application.domain.services.inventory.ReceiveStockService;
import application.domain.services.inventory.ReleaseReservationService;
import application.domain.services.inventory.ReserveStockService;
import application.domain.services.inventory.RestockReturnService;
import application.domain.services.invoice.IssueInvoiceService;
import application.domain.services.order.CompleteDeliveryService;
import application.domain.services.order.ConfirmPaymentService;
import application.domain.services.order.ConsultOrderService;
import application.domain.services.order.DispatchOrderService;
import application.domain.services.refund.IssueRefundService;
import application.domain.services.returns.RequestReturnService;

/**
 * Wires the domain services against in-memory doubles, so a test only states what it is exercising.
 *
 * <p>No mocking framework: the services run against fakes that really store state, which keeps the
 * assertions about observable outcomes — the stock that changed, the log that was written — instead
 * of about which method happened to be called.</p>
 */
public final class ServiceHarness {

    public final InMemoryUserRepository users = new InMemoryUserRepository();

    public final InMemorySellerRepository sellers = new InMemorySellerRepository();

    public final InMemoryWarehouseRepository warehouses = new InMemoryWarehouseRepository();

    public final InMemoryProductRepository products = new InMemoryProductRepository();

    public final InMemoryInventoryRepositories inventory = new InMemoryInventoryRepositories();

    public final InMemoryCommerceRepositories commerce = new InMemoryCommerceRepositories();

    public final InMemoryOperationLogRepository operationLogs = new InMemoryOperationLogRepository();

    public final ValidateUserActiveStatusService validateUserActiveStatus = new ValidateUserActiveStatusService();

    public final ValidateRoleAuthorizationService validateRole =
            new ValidateRoleAuthorizationService(validateUserActiveStatus);

    public final ValidateBuyerOwnershipService validateBuyerOwnership = new ValidateBuyerOwnershipService();

    public final ValidateSellerOwnershipService validateSellerOwnership = new ValidateSellerOwnershipService();

    public final RegisterOperationLogService registerOperationLog = new RegisterOperationLogService(operationLogs);

    private final InventoryItemRepositoryPort inventoryItems = inventory.items();

    private final InventoryMovementRepositoryPort inventoryMovements = inventory.movements();

    private final CartRepositoryPort carts = commerce.carts();

    private final OrderRepositoryPort orders = commerce.orders();

    private final InvoiceRepositoryPort invoices = commerce.invoices();

    private final ShipmentRepositoryPort shipments = commerce.shipments();

    private final ProductReturnRepositoryPort returns = commerce.returns();

    private final RefundRepositoryPort refunds = commerce.refunds();

    private final ProductRepositoryPort productPort = products;

    // Catalogue

    public RegisterProductService registerProduct() {
        return new RegisterProductService(productPort, validateRole, validateSellerOwnership);
    }

    public PublishProductService publishProduct() {
        return new PublishProductService(productPort, validateRole, validateSellerOwnership);
    }

    public SuspendProductService suspendProduct() {
        return new SuspendProductService(productPort, validateRole, validateSellerOwnership);
    }

    public DiscontinueProductService discontinueProduct() {
        return new DiscontinueProductService(productPort, validateRole, validateSellerOwnership);
    }

    public AddProductVariantService addProductVariant() {
        return new AddProductVariantService(productPort, validateRole, validateSellerOwnership);
    }

    public RemoveProductVariantService removeProductVariant() {
        return new RemoveProductVariantService(productPort, validateRole, validateSellerOwnership);
    }

    public ConsultCatalogService consultCatalog() {
        return new ConsultCatalogService(productPort, validateRole);
    }

    // Inventory

    public OpenStockRecordService openStockRecord() {
        return new OpenStockRecordService(inventoryItems, validateRole, validateSellerOwnership);
    }

    public ReceiveStockService receiveStock() {
        return new ReceiveStockService(inventoryItems, inventoryMovements, validateRole);
    }

    public ReserveStockService reserveStock() {
        return new ReserveStockService(inventoryItems, inventoryMovements, validateRole);
    }

    public ReleaseReservationService releaseReservation() {
        return new ReleaseReservationService(inventoryItems, inventoryMovements, validateRole);
    }

    public IssueForSaleService issueForSale() {
        return new IssueForSaleService(inventoryItems, inventoryMovements, validateRole);
    }

    public AdjustStockService adjustStock() {
        return new AdjustStockService(inventoryItems, inventoryMovements, validateRole, registerOperationLog);
    }

    public MarkDamagedService markDamaged() {
        return new MarkDamagedService(inventoryItems, validateRole);
    }

    public RestockReturnService restockReturn() {
        return new RestockReturnService(inventoryItems, inventoryMovements, validateRole);
    }

    public ConsultInventoryService consultInventory() {
        return new ConsultInventoryService(inventoryItems, inventoryMovements, validateRole);
    }

    // Cart

    public AddCartItemService addCartItem() {
        return new AddCartItemService(carts, validateRole, validateBuyerOwnership);
    }

    public RemoveCartItemService removeCartItem() {
        return new RemoveCartItemService(carts, validateRole, validateBuyerOwnership);
    }

    public UpdateCartItemQuantityService updateCartItemQuantity() {
        return new UpdateCartItemQuantityService(carts, validateRole, validateBuyerOwnership);
    }

    public ClearCartService clearCart() {
        return new ClearCartService(carts, validateRole, validateBuyerOwnership);
    }

    public ConfirmCartService confirmCart() {
        return new ConfirmCartService(carts, orders, validateRole, validateBuyerOwnership);
    }

    // Order

    public ConfirmPaymentService confirmPayment() {
        return new ConfirmPaymentService(orders, validateRole, validateBuyerOwnership, registerOperationLog);
    }

    public DispatchOrderService dispatchOrder() {
        return new DispatchOrderService(orders, validateRole, registerOperationLog);
    }

    public CompleteDeliveryService completeDelivery() {
        return new CompleteDeliveryService(orders, validateRole, registerOperationLog);
    }

    public ConsultOrderService consultOrder() {
        return new ConsultOrderService(orders, validateRole, validateBuyerOwnership);
    }

    // Invoicing and after sales

    public IssueInvoiceService issueInvoice() {
        return new IssueInvoiceService(invoices, validateRole);
    }

    public RequestReturnService requestReturn() {
        return new RequestReturnService(returns, validateRole, validateBuyerOwnership);
    }

    public IssueRefundService issueRefund() {
        return new IssueRefundService(refunds, validateRole, registerOperationLog);
    }

    public ShipmentRepositoryPort shipments() {
        return shipments;
    }

    public InventoryItemRepositoryPort inventoryItems() {
        return inventoryItems;
    }

    public OrderRepositoryPort orders() {
        return orders;
    }

    public CartRepositoryPort carts() {
        return carts;
    }
}
