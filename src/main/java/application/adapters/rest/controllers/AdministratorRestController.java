package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.ChangeUserRoleRequest;
import application.adapters.rest.dtos.requests.IssueInvoiceRequest;
import application.adapters.rest.dtos.requests.IssueRefundRequest;
import application.adapters.rest.dtos.requests.RegisterSellerRequest;
import application.adapters.rest.dtos.requests.RegisterWarehouseRequest;
import application.adapters.rest.dtos.responses.InvoiceResponse;
import application.adapters.rest.dtos.responses.ProductReturnResponse;
import application.adapters.rest.dtos.responses.RefundResponse;
import application.adapters.rest.dtos.responses.SellerResponse;
import application.adapters.rest.dtos.responses.UserResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.ProductReturn;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
import application.domain.ports.in.AdministratorPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.services.invoice.IssueInvoiceService;
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
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator endpoints: seller incorporation, user status and role, marketplace warehouses,
 * invoicing and refunds.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdministratorRestController {

    private final AdministratorPort administratorPort;

    private final IssueInvoiceService issueInvoiceService;

    private final UserRepositoryPort userRepositoryPort;

    private final OrderRepositoryPort orderRepositoryPort;

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    private final ProductReturnRepositoryPort productReturnRepositoryPort;

    private final CredentialService credentialService;

    public AdministratorRestController(AdministratorPort administratorPort,
                                       IssueInvoiceService issueInvoiceService,
                                       UserRepositoryPort userRepositoryPort,
                                       OrderRepositoryPort orderRepositoryPort,
                                       InvoiceRepositoryPort invoiceRepositoryPort,
                                       ProductReturnRepositoryPort productReturnRepositoryPort,
                                       CredentialService credentialService) {
        this.administratorPort = Objects.requireNonNull(administratorPort);
        this.issueInvoiceService = Objects.requireNonNull(issueInvoiceService);
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort);
        this.orderRepositoryPort = Objects.requireNonNull(orderRepositoryPort);
        this.invoiceRepositoryPort = Objects.requireNonNull(invoiceRepositoryPort);
        this.productReturnRepositoryPort = Objects.requireNonNull(productReturnRepositoryPort);
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

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponse>> consultSellers(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(administratorPort.consultSellers(principal.user()).stream()
                .map(UserRestMapper::toResponse)
                .toList());
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<SellerResponse> consultSeller(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @PathVariable String sellerId) {
        return ResponseEntity.ok(UserRestMapper.toResponse(
                administratorPort.consultSeller(principal.user(), SellerId.of(sellerId))));
    }

    // Users

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<UserResponse> blockUser(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                  @PathVariable String userId) {
        return ResponseEntity.ok(
                UserRestMapper.toResponse(administratorPort.blockUser(principal.user(), userOf(userId))));
    }

    @PostMapping("/users/{userId}/activation")
    public ResponseEntity<UserResponse> activateUser(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                     @PathVariable String userId) {
        return ResponseEntity.ok(
                UserRestMapper.toResponse(administratorPort.activateUser(principal.user(), userOf(userId))));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> changeUserRole(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                       @PathVariable String userId,
                                                       @Valid @RequestBody ChangeUserRoleRequest request) {
        return ResponseEntity.ok(UserRestMapper.toResponse(administratorPort.changeUserRole(
                principal.user(), userOf(userId), UserRole.valueOf(request.role()))));
    }

    // Marketplace warehouses

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
     * Invoicing is reachable by the administrator and the seller (RD-FAC-01), but it is not part of
     * {@code AdministratorPort}, so the domain service is injected directly. The service still
     * validates the role, which is where the rule lives.
     */
    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> issueInvoice(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @Valid @RequestBody IssueInvoiceRequest request) {
        Order order = orderRepositoryPort.findById(OrderId.of(request.orderId()))
                .orElseThrow(() -> new EntityNotFoundException("order", OrderId.of(request.orderId())));
        Invoice invoice = issueInvoiceService.issue(principal.user(), order, InvoiceId.of(request.invoiceId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AfterSalesRestMapper.toResponse(invoice));
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
        ProductReturn productReturn = productReturnRepositoryPort
                .findById(ProductReturnId.of(request.productReturnId()))
                .orElseThrow(() -> new EntityNotFoundException("product return",
                        ProductReturnId.of(request.productReturnId())));
        Invoice invoice = invoiceRepositoryPort.findById(InvoiceId.of(request.invoiceId()))
                .orElseThrow(() -> new EntityNotFoundException("invoice", InvoiceId.of(request.invoiceId())));
        RefundResponse response = AfterSalesRestMapper.toResponse(administratorPort.issueRefund(
                principal.user(), productReturn, invoice,
                Money.of(request.amount(), request.currency()), RefundId.of(request.refundId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<RefundResponse>> consultRefunds(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        return ResponseEntity.ok(administratorPort.consultRefunds(principal.user()).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList());
    }

    private User<?> userOf(String userId) {
        return userRepositoryPort.findByIdentifier(UserId.of(userId))
                .orElseThrow(() -> new EntityNotFoundException("user", UserId.of(userId)));
    }
}
