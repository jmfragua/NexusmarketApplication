package application.adapters.useCases;

import application.domain.models.Invoice;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
import application.domain.ports.in.AdministratorPort;
import application.domain.services.refund.ConsultRefundService;
import application.domain.services.refund.IssueRefundService;
import application.domain.services.returns.ConsultReturnService;
import application.domain.services.seller.ConsultSellerService;
import application.domain.services.seller.RegisterSellerService;
import application.domain.services.user.ActivateUserService;
import application.domain.services.user.BlockUserService;
import application.domain.services.user.ChangeUserRoleService;
import application.domain.services.warehouse.RegisterMarketplaceWarehouseService;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserRole;
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

    private final BlockUserService blockUserService;

    private final ActivateUserService activateUserService;

    private final ChangeUserRoleService changeUserRoleService;

    private final RegisterMarketplaceWarehouseService registerMarketplaceWarehouseService;

    private final IssueRefundService issueRefundService;

    private final ConsultRefundService consultRefundService;

    private final ConsultReturnService consultReturnService;

    public AdministratorUseCaseImpl(RegisterSellerService registerSellerService,
                                    ConsultSellerService consultSellerService,
                                    BlockUserService blockUserService,
                                    ActivateUserService activateUserService,
                                    ChangeUserRoleService changeUserRoleService,
                                    RegisterMarketplaceWarehouseService registerMarketplaceWarehouseService,
                                    IssueRefundService issueRefundService,
                                    ConsultRefundService consultRefundService,
                                    ConsultReturnService consultReturnService) {
        this.registerSellerService = Objects.requireNonNull(registerSellerService);
        this.consultSellerService = Objects.requireNonNull(consultSellerService);
        this.blockUserService = Objects.requireNonNull(blockUserService);
        this.activateUserService = Objects.requireNonNull(activateUserService);
        this.changeUserRoleService = Objects.requireNonNull(changeUserRoleService);
        this.registerMarketplaceWarehouseService = Objects.requireNonNull(registerMarketplaceWarehouseService);
        this.issueRefundService = Objects.requireNonNull(issueRefundService);
        this.consultRefundService = Objects.requireNonNull(consultRefundService);
        this.consultReturnService = Objects.requireNonNull(consultReturnService);
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
}
