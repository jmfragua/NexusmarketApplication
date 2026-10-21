package application.domain.ports.in;

import application.domain.models.Invoice;
import application.domain.models.MarketplaceWarehouse;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.Identifier;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserId;
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

    // Incorporation of staff: logistics operators and supervisors (RD-ROL-09)

    StaffUser registerStaffUser(User<?> administrator,
                                UserId userId,
                                FullName fullName,
                                EmailAddress email,
                                IdentityDocument identityDocument,
                                UserRole role);

    // Operational status and role of the users

    /**
     * Resolves a user by its identifier, which the status and role operations act upon.
     */
    User<?> consultUser(User<?> administrator, Identifier userId);

    User<?> blockUser(User<?> administrator, User<?> target);

    User<?> activateUser(User<?> administrator, User<?> target);

    User<?> changeUserRole(User<?> administrator, User<?> target, UserRole newRole);

    // Warehouses of the marketplace

    MarketplaceWarehouse registerMarketplaceWarehouse(User<?> administrator, WarehouseId warehouseId);

    // After sales

    /**
     * Issues the invoice of a paid order (RD-FAC-01). Shared with the seller, but reachable here so
     * the administrator never has to step outside this port.
     */
    Invoice issueInvoice(User<?> administrator, OrderId orderId, InvoiceId invoiceId);

    /**
     * Resolves an invoice by its identifier, which a refund is applied over.
     */
    Invoice consultInvoice(User<?> administrator, InvoiceId invoiceId);

    Refund issueRefund(User<?> administrator, ProductReturn productReturn, Invoice invoice, Money amount,
                       RefundId refundId);

    List<Refund> consultRefunds(User<?> administrator);

    // Read only listings of the information the administrator manages (OBJ-01, OBJ-02, OBJ-04)

    List<User<?>> consultUsers(User<?> administrator);

    List<Warehouse> consultWarehouses(User<?> administrator);

    /**
     * Resolves the invoice of an order, which a refund is applied upon (RD-POS-02).
     */
    Invoice consultInvoiceOfOrder(User<?> administrator, OrderId orderId);

    ProductReturn consultReturn(User<?> administrator, ProductReturnId returnId);
}
