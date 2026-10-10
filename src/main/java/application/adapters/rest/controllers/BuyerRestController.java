package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.AddCartItemRequest;
import application.adapters.rest.dtos.requests.AddressRequest;
import application.adapters.rest.dtos.requests.ConfirmCartRequest;
import application.adapters.rest.dtos.requests.RequestReturnRequest;
import application.adapters.rest.dtos.requests.UpdateCartItemQuantityRequest;
import application.adapters.rest.dtos.responses.BuyerResponse;
import application.adapters.rest.dtos.responses.CartResponse;
import application.adapters.rest.dtos.responses.InvoiceResponse;
import application.adapters.rest.dtos.responses.OrderResponse;
import application.adapters.rest.dtos.responses.PageResponse;
import application.adapters.rest.dtos.responses.ProductReturnResponse;
import application.adapters.rest.mappers.AfterSalesRestMapper;
import application.adapters.rest.mappers.CartRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.UserRestMapper;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Order;
import application.domain.models.OrderLine;
import application.domain.ports.in.BuyerPort;
import application.domain.ports.in.PublicAccessPort;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.Quantity;
import application.infrastructure.security.ActorResolver;
import application.infrastructure.security.DomainUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Buyer endpoints: own addresses, cart, orders, invoices and returns.
 *
 * <p>Entities referenced by identifier are resolved through the ports themselves — never through a
 * repository — so the ownership validation of each consultation runs before the entity reaches the
 * operation. A buyer asking for someone else's order is rejected while loading it, not afterwards
 * (RD-ROL-04, RD-PED-05).</p>
 */
@RestController
@RequestMapping("/api/v1/buyer")
public class BuyerRestController {

    private final BuyerPort buyerPort;

    private final PublicAccessPort publicAccessPort;

    public BuyerRestController(BuyerPort buyerPort, PublicAccessPort publicAccessPort) {
        this.buyerPort = Objects.requireNonNull(buyerPort);
        this.publicAccessPort = Objects.requireNonNull(publicAccessPort);
    }

    // Profile and delivery addresses

    @PostMapping("/me/addresses")
    public ResponseEntity<BuyerResponse> addAdditionalAddress(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                              @Valid @RequestBody AddressRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(
                UserRestMapper.toResponse(buyerPort.addAdditionalAddress(buyer, Address.of(request.address()))));
    }

    @PutMapping("/me/addresses/primary")
    public ResponseEntity<BuyerResponse> changePrimaryAddress(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                              @Valid @RequestBody AddressRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(
                UserRestMapper.toResponse(buyerPort.changePrimaryAddress(buyer, Address.of(request.address()))));
    }

    // Cart

    @GetMapping("/me/cart")
    public ResponseEntity<CartResponse> consultMyCart(@AuthenticationPrincipal DomainUserPrincipal principal) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(CartRestMapper.toResponse(buyerPort.consultMyCart(buyer)));
    }

    @PostMapping("/me/cart/items")
    public ResponseEntity<CartResponse> addCartItem(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                    @Valid @RequestBody AddCartItemRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Cart cart = buyerPort.consultMyCart(buyer);
        buyerPort.addCartItem(buyer, cart,
                publicAccessPort.consultPublishedProduct(ProductId.of(request.productId())),
                ProductVariantId.of(request.productVariantId()),
                Quantity.of(request.quantity()),
                CartItemId.of(request.cartItemId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CartRestMapper.toResponse(cart));
    }

    @PatchMapping("/me/cart/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Cart cart = buyerPort.consultMyCart(buyer);
        CartItem item = itemOf(cart, cartItemId);
        return ResponseEntity.ok(CartRestMapper.toResponse(
                buyerPort.updateCartItemQuantity(buyer, cart, item, Quantity.of(request.quantity()))));
    }

    @DeleteMapping("/me/cart/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                       @PathVariable String cartItemId) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Cart cart = buyerPort.consultMyCart(buyer);
        return ResponseEntity.ok(
                CartRestMapper.toResponse(buyerPort.removeCartItem(buyer, cart, itemOf(cart, cartItemId))));
    }

    @DeleteMapping("/me/cart")
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal DomainUserPrincipal principal) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(CartRestMapper.toResponse(buyerPort.clearCart(buyer, buyerPort.consultMyCart(buyer))));
    }

    // Orders

    @PostMapping("/me/orders")
    public ResponseEntity<OrderResponse> confirmCart(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                     @Valid @RequestBody ConfirmCartRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Cart cart = buyerPort.consultMyCart(buyer);
        OrderId orderId = OrderId.of(request.orderId());
        List<OrderLine> lines = request.lines().stream()
                .map(line -> OrderRestMapper.toLineDomain(line, orderId))
                .toList();
        Order order = buyerPort.confirmCart(buyer, cart, orderId, Address.of(request.deliveryAddress()), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderRestMapper.toResponse(order));
    }

    @PostMapping("/me/orders/{orderId}/payment")
    public ResponseEntity<OrderResponse> confirmPayment(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @PathVariable String orderId) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Order order = buyerPort.consultMyOrder(buyer, OrderId.of(orderId));
        return ResponseEntity.ok(OrderRestMapper.toResponse(buyerPort.confirmPayment(buyer, order)));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<PageResponse<OrderResponse>> consultMyOrders(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(PageResponse.of(buyerPort.consultMyOrders(buyer).stream()
                .map(OrderRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/me/orders/{orderId}")
    public ResponseEntity<OrderResponse> consultMyOrder(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                        @PathVariable String orderId) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(
                OrderRestMapper.toResponse(buyerPort.consultMyOrder(buyer, OrderId.of(orderId))));
    }

    // Invoicing

    @GetMapping("/me/invoices")
    public ResponseEntity<PageResponse<InvoiceResponse>> consultMyInvoices(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        return ResponseEntity.ok(PageResponse.of(buyerPort.consultMyInvoices(buyer).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/me/orders/{orderId}/invoice")
    public ResponseEntity<InvoiceResponse> consultMyInvoice(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                            @PathVariable String orderId) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Order order = buyerPort.consultMyOrder(buyer, OrderId.of(orderId));
        return ResponseEntity.ok(AfterSalesRestMapper.toResponse(buyerPort.consultMyInvoice(buyer, order)));
    }

    // After sales

    @PostMapping("/me/returns")
    public ResponseEntity<ProductReturnResponse> requestReturn(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                               @Valid @RequestBody RequestReturnRequest request) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Order order = buyerPort.consultMyOrder(buyer, OrderId.of(request.orderId()));
        OrderLine line = lineOf(order, request.orderLineId());
        ProductReturnResponse response = AfterSalesRestMapper.toResponse(buyerPort.requestReturn(
                buyer, order, line, Quantity.of(request.quantity()), ProductReturnId.of(request.returnId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/orders/{orderId}/returns")
    public ResponseEntity<PageResponse<ProductReturnResponse>> consultMyReturns(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Buyer buyer = ActorResolver.requireBuyer(principal.user());
        Order order = buyerPort.consultMyOrder(buyer, OrderId.of(orderId));
        return ResponseEntity.ok(PageResponse.of(buyerPort.consultMyReturns(buyer, order).stream()
                .map(AfterSalesRestMapper::toResponse)
                .toList(), page, size));
    }

    /**
     * Looks the line up inside the aggregate that owns it, rather than loading it on its own: a cart
     * line has no repository because it is not an aggregate root.
     */
    private CartItem itemOf(Cart cart, String cartItemId) {
        return cart.getItems().stream()
                .filter(item -> item.getIdentifier().getValue().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "cart line " + cartItemId + " does not belong to the cart of this buyer"));
    }

    private OrderLine lineOf(Order order, String orderLineId) {
        return order.getLines().stream()
                .filter(line -> line.getIdentifier().getValue().equals(orderLineId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "order line " + orderLineId + " does not belong to order " + order.getIdentifier()));
    }
}
