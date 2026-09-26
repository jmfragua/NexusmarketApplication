package application.domain.ports.in;

import application.domain.models.Invoice;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
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

/**
 * Entry contract of the administrator role.
 *
 * <p>Incorporates sellers along with their first warehouse (RD-ROL-05), manages the operational
 * status of the users (RG-01) and the warehouses of the marketplace, and manages the refunds
 * derived from the returns (RD-ROL-06, RD-POS-02).</p>
 */
public interface AdministratorPort {

    // Incorporation of sellers: never a self registration (RD-ROL-05)

    Seller registerSeller(User<?> administrator,
                          SellerId sellerId,
                          FullName fullName,
                          EmailAddress email,
                          IdentityDocument identityDocument,
                          UserStatus status,
                          SellerWarehouse firstWarehouse);

    Seller consultSeller(User<?> administrator, SellerId sellerId);

    List<Seller> consultSellers(User<?> administrator);

    // Operational status and role of the users

    User<?> blockUser(User<?> administrator, User<?> target);

    User<?> activateUser(User<?> administrator, User<?> target);

    User<?> changeUserRole(User<?> administrator, User<?> target, UserRole newRole);

    // Warehouses of the marketplace

    MarketplaceWarehouse registerMarketplaceWarehouse(User<?> administrator, WarehouseId warehouseId);

    // After sales

    Refund issueRefund(User<?> administrator, ProductReturn productReturn, Invoice invoice, Money amount,
                       RefundId refundId);

    List<Refund> consultRefunds(User<?> administrator);

    ProductReturn consultReturn(User<?> administrator, ProductReturnId returnId);
}
