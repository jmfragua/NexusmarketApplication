package application.adapters.useCases;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Invoice;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.Order;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.AdministratorPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.services.invoice.ConsultInvoiceService;
import application.domain.services.invoice.IssueInvoiceService;
import application.domain.services.refund.ConsultRefundService;
import application.domain.services.refund.IssueRefundService;
import application.domain.services.returns.ConsultReturnService;
import application.domain.services.seller.ConsultSellerService;
import application.domain.services.seller.RegisterSellerService;
import application.domain.services.user.ActivateUserService;
import application.domain.services.user.BlockUserService;
import application.domain.services.user.ChangeUserRoleService;
import application.domain.services.user.RegisterStaffUserService;
import application.domain.services.warehouse.ConsultWarehouseService;
import application.domain.services.warehouse.RegisterMarketplaceWarehouseService;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implements the administrator entry port by delegating to the domain services.
 */
@Service
public class AdministratorUseCaseImpl implements AdministratorPort {

    private final RegisterSellerService registerSellerService;

    private final ConsultSellerService consultSellerService;

    private final RegisterStaffUserService registerStaffUserService;

    private final BlockUserService blockUserService;

    private final ActivateUserService activateUserService;

    private final ChangeUserRoleService changeUserRoleService;

    private final RegisterMarketplaceWarehouseService registerMarketplaceWarehouseService;

    private final IssueRefundService issueRefundService;

    private final ConsultRefundService consultRefundService;

    private final ConsultReturnService consultReturnService;

    private final IssueInvoiceService issueInvoiceService;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final UserRepositoryPort userRepositoryPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    private final ConsultWarehouseService consultWarehouseService;

    private final ConsultInvoiceService consultInvoiceService;

    public AdministratorUseCaseImpl(RegisterSellerService registerSellerService,
                                    ConsultSellerService consultSellerService,
                                    RegisterStaffUserService registerStaffUserService,
                                    BlockUserService blockUserService,
                                    ActivateUserService activateUserService,
                                    ChangeUserRoleService changeUserRoleService,
                                    RegisterMarketplaceWarehouseService registerMarketplaceWarehouseService,
                                    IssueRefundService issueRefundService,
                                    ConsultRefundService consultRefundService,
                                    ConsultReturnService consultReturnService,
                                    IssueInvoiceService issueInvoiceService,
                                    ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                    UserRepositoryPort userRepositoryPort,
                                    OrderRepositoryPort orderRepositoryPort,
                                    InvoiceRepositoryPort invoiceRepositoryPort,
                                    ConsultWarehouseService consultWarehouseService,
                                    ConsultInvoiceService consultInvoiceService) {
        this.registerSellerService = Objects.requireNonNull(registerSellerService);
        this.consultSellerService = Objects.requireNonNull(consultSellerService);
        this.registerStaffUserService = Objects.requireNonNull(registerStaffUserService);
        this.blockUserService = Objects.requireNonNull(blockUserService);
        this.activateUserService = Objects.requireNonNull(activateUserService);
        this.changeUserRoleService = Objects.requireNonNull(changeUserRoleService);
        this.registerMarketplaceWarehouseService = Objects.requireNonNull(registerMarketplaceWarehouseService);
        this.issueRefundService = Objects.requireNonNull(issueRefundService);
        this.consultRefundService = Objects.requireNonNull(consultRefundService);
        this.consultReturnService = Objects.requireNonNull(consultReturnService);
        this.issueInvoiceService = Objects.requireNonNull(issueInvoiceService);
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService);
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort);
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort);
        this.invoiceRepositoryPort = Objects.requireNonNull(invoiceRepositoryPort);
        this.consultWarehouseService = Objects.requireNonNull(consultWarehouseService);
        this.consultInvoiceService = Objects.requireNonNull(consultInvoiceService);
    }

    /**
     * Only the administrator manages the status and the role of the users (RG-01, RD-ROL-06), so the
     * resolution is gated by that role instead of being a free lookup.
     */
    @Override
    public User<?> consultUser(User<?> administrator, Identifier userId) {
        validateRoleAuthorizationService.validate(administrator, "consult user", UserRole.ADMINISTRATOR);
        return userRepositoryPort.findByIdentifier(userId)
                .orElseThrow(() -> new EntityNotFoundException("user", userId));
    }

    /**
     * The role is validated before the order is resolved, not only by {@code IssueInvoiceService}
     * afterwards: otherwise an actor who may not invoice would still learn from the status code
     * whether the order exists (RD-ROL-03).
     */
    @Override
    public Invoice issueInvoice(User<?> administrator, OrderId orderId, InvoiceId invoiceId) {
        validateRoleAuthorizationService.validate(administrator, "issue invoice", UserRole.SELLER,
                UserRole.ADMINISTRATOR);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order", orderId));
        return issueInvoiceService.issue(administrator, order, invoiceId);
    }

    @Override
    public Invoice consultInvoice(User<?> administrator, InvoiceId invoiceId) {
        validateRoleAuthorizationService.validate(administrator, "consult invoice", UserRole.SELLER,
                UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return invoiceRepositoryPort.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("invoice", invoiceId));
    }

    @Override
    public Seller registerSeller(User<?> administrator, SellerId sellerId, FullName fullName, EmailAddress email,
                                 IdentityDocument identityDocument, UserStatus status,
                                 SellerWarehouse firstWarehouse) {
        return registerSellerService.register(administrator, sellerId, fullName, email, identityDocument, status,
                firstWarehouse);
    }

    @Override
    public Seller consultSeller(User<?> administrator, SellerId sellerId) {
        return consultSellerService.consult(administrator, sellerId);
    }

    @Override
    public List<Seller> consultSellers(User<?> administrator) {
        return consultSellerService.consultAll(administrator);
    }

    @Override
    public User<?> blockUser(User<?> administrator, User<?> target) {
        return blockUserService.block(administrator, target);
    }

    @Override
    public User<?> activateUser(User<?> administrator, User<?> target) {
        return activateUserService.activate(administrator, target);
    }

    @Override
    public StaffUser registerStaffUser(User<?> administrator, UserId userId, FullName fullName, EmailAddress email,
                                       IdentityDocument identityDocument, UserRole role) {
        return registerStaffUserService.register(administrator, userId, fullName, email, identityDocument, role);
    }

    @Override
    public User<?> changeUserRole(User<?> administrator, User<?> target, UserRole newRole) {
        return changeUserRoleService.changeRole(administrator, target, newRole);
    }

    @Override
    public MarketplaceWarehouse registerMarketplaceWarehouse(User<?> administrator, WarehouseId warehouseId) {
        return registerMarketplaceWarehouseService.register(administrator, warehouseId);
    }

    @Override
    public Refund issueRefund(User<?> administrator, ProductReturn productReturn, Invoice invoice, Money amount,
                              RefundId refundId) {
        return issueRefundService.issue(administrator, productReturn, invoice, amount, refundId);
    }

    @Override
    public List<Refund> consultRefunds(User<?> administrator) {
        return consultRefundService.consultAll(administrator);
    }

    @Override
    public ProductReturn consultReturn(User<?> administrator, ProductReturnId returnId) {
        return consultReturnService.consult(administrator, returnId);
    }

    @Override
    public List<User<?>> consultUsers(User<?> administrator) {
        validateRoleAuthorizationService.validate(administrator, "consult users", UserRole.ADMINISTRATOR);
        return userRepositoryPort.findAll();
    }

    @Override
    public List<Warehouse> consultWarehouses(User<?> administrator) {
        return consultWarehouseService.consultAll(administrator);
    }

    /**
     * The role is checked before the order is resolved, so a caller without it cannot learn from the
     * status code whether an order exists.
     */
    @Override
    public Invoice consultInvoiceOfOrder(User<?> administrator, OrderId orderId) {
        validateRoleAuthorizationService.validate(administrator, "consult invoice of order", UserRole.ADMINISTRATOR);
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("order", orderId));
        return consultInvoiceService.consultByOrder(administrator, order);
    }
}
