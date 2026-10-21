package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.ChangeUserRoleRequest;
import application.adapters.rest.dtos.requests.IssueInvoiceRequest;
import application.adapters.rest.dtos.requests.IssueRefundRequest;
import application.adapters.rest.dtos.requests.RegisterSellerRequest;
import application.adapters.rest.dtos.requests.RegisterStaffRequest;
import application.adapters.rest.dtos.requests.RegisterWarehouseRequest;
import application.adapters.rest.dtos.responses.InvoiceResponse;
import application.adapters.rest.dtos.responses.PageResponse;
import application.adapters.rest.dtos.responses.ProductReturnResponse;
import application.adapters.rest.dtos.responses.RefundResponse;
import application.adapters.rest.dtos.responses.SellerResponse;
import application.adapters.rest.dtos.responses.UserResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.ports.in.AdministratorPort;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import application.domain.valuesObjects.WarehouseId;
import application.infrastructure.security.CredentialService;
import application.infrastructure.security.DomainUserPrincipal;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator endpoints: seller incorporation, user status and role, marketplace warehouses,
 * invoicing and refunds.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdministratorRestController {

    private final AdministratorPort administratorPort;

    private final CredentialService credentialService;

    public AdministratorRestController(AdministratorPort administratorPort,
                                       CredentialService credentialService) {
        this.administratorPort = Objects.requireNonNull(administratorPort);
        this.credentialService = Objects.requireNonNull(credentialService);
    }

    // Sellers

    /**
     * Incorporates a seller together with their first warehouse — the only way a seller comes into
     * existence (RD-ROL-05). The credential is stored afterwards, so a rejected incorporation leaves
     * no orphan password.
     */
    @PostMapping("/sellers")
    public ResponseEntity<SellerResponse> registerSeller(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                         @Valid @RequestBody RegisterSellerRequest request) {
        SellerId sellerId = SellerId.of(request.sellerId());
        SellerWarehouse firstWarehouse = new SellerWarehouse(
                WarehouseId.of(request.firstWarehouseId()), sellerId);
        Seller seller = administratorPort.registerSeller(
                principal.user(),
                sellerId,
                FullName.of(request.fullName()),
                EmailAddress.of(request.email()),
                IdentityDocument.of(request.identityDocument()),
                UserStatus.ACTIVE,
                firstWarehouse);
        credentialService.register(seller.getEmail(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserRestMapper.toResponse(seller));
    }

    // Staff

    /**
     * Incorporates a logistics operator or a supervisor (RD-ROL-09). As with sellers, the credential
     * is stored only after the domain accepted the user, so a rejection leaves no orphan password.
     */
    @PostMapping("/staff")
    public ResponseEntity<UserResponse> registerStaff(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                      @Valid @RequestBody RegisterStaffRequest request) {
        StaffUser staffUser = administratorPort.registerStaffUser(
                principal.user(),
                UserId.of(request.userId()),
                FullName.of(request.fullName()),
                EmailAddress.of(request.email()),
                IdentityDocument.of(request.identityDocument()),
                UserRole.valueOf(request.role()));
        credentialService.register(staffUser.getEmail(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserRestMapper.toResponse(staffUser));
    }

    @GetMapping("/sellers")
    public ResponseEntity<PageResponse<SellerResponse>> consultSellers(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(administratorPort.consultSellers(principal.user()).stream()
                .map(UserRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<SellerResponse> consultSeller(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @PathVariable String sellerId) {
        return ResponseEntity.ok(UserRestMapper.toResponse(
                administratorPort.consultSeller(principal.user(), SellerId.of(sellerId))));
    }

    // Users

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> consultUsers(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(administratorPort.consultUsers(principal.user()).stream()
                .map(user -> UserRestMapper.toResponse((User<?>) user))
                .toList(), page, size));
    }

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<UserResponse> blockUser(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                  @PathVariable String userId) {
        return ResponseEntity.ok(
                UserRestMapper.toResponse(administratorPort.blockUser(principal.user(), userOf(principal, userId))));
    }

    @PostMapping("/users/{userId}/activation")
    public ResponseEntity<UserResponse> activateUser(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                     @PathVariable String userId) {
        return ResponseEntity.ok(
                UserRestMapper.toResponse(administratorPort.activateUser(principal.user(), userOf(principal, userId))));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> changeUserRole(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                       @PathVariable String userId,
                                                       @Valid @RequestBody ChangeUserRoleRequest request) {
        return ResponseEntity.ok(UserRestMapper.toResponse(administratorPort.changeUserRole(
                principal.user(), userOf(principal, userId), UserRole.valueOf(request.role()))));
    }

    // Marketplace warehouses

    @GetMapping("/warehouses")
    public ResponseEntity<PageResponse<WarehouseResponse>> consultWarehouses(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(administratorPort.consultWarehouses(principal.user()).stream()
                .map(InventoryRestMapper::toResponse)
                .toList(), page, size));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<WarehouseResponse> registerMarketplaceWarehouse(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody RegisterWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(
                administratorPort.registerMarketplaceWarehouse(
                        principal.user(), WarehouseId.of(request.warehouseId()))));
    }

    // Invoicing

    /**
     * Invoicing is reachable by the administrator and the seller (RD-FAC-01); the entry port exposes
     * it so the administrator never has to step outside it, and the domain service behind it still
     * validates the role, which is where the rule lives.
     */
    /**
     * The invoice of an order, which a refund is applied upon (RD-POS-02). 404 when the order does
     * not exist or has not been invoiced yet.
     */
    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<InvoiceResponse> consultInvoiceOfOrder(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                                 @PathVariable String orderId) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(
                administratorPort.consultInvoiceOfOrder(principal.user(), OrderId.of(orderId))));
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> issueInvoice(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @Valid @RequestBody IssueInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AfterSalesRestMapper.toResponse(
                administratorPort.issueInvoice(principal.user(), OrderId.of(request.orderId()),
                        InvoiceId.of(request.invoiceId()))));
    }

    // After sales

    @GetMapping("/returns/{returnId}")
    public ResponseEntity<ProductReturnResponse> consultReturn(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                               @PathVariable String returnId) {
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(
                administratorPort.consultReturn(principal.user(), ProductReturnId.of(returnId))));
    }

    @PostMapping("/refunds")
    public ResponseEntity<RefundResponse> issueRefund(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                      @Valid @RequestBody IssueRefundRequest request) {
        RefundResponse response = AfterSalesRestMapper.toResponse(administratorPort.issueRefund(
                principal.user(),
                administratorPort.consultReturn(principal.user(), ProductReturnId.of(request.productReturnId())),
                administratorPort.consultInvoice(principal.user(), InvoiceId.of(request.invoiceId())),
                Money.of(request.amount(), request.currency()), RefundId.of(request.refundId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/refunds")
    public ResponseEntity<PageResponse<RefundResponse>> consultRefunds(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(administratorPort.consultRefunds(principal.user()).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList(), page, size));
    }

    private User<?> userOf(DomainUserPrincipal principal, String userId) {
        return administratorPort.consultUser(principal.user(), UserId.of(userId));
    }
}
