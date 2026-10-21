package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.AddProductVariantRequest;
import application.adapters.rest.dtos.requests.OpenStockRecordRequest;
import application.adapters.rest.dtos.requests.RegisterProductRequest;
import application.adapters.rest.dtos.requests.StockMovementRequest;
import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
import application.adapters.rest.dtos.responses.PageResponse;
import application.adapters.rest.dtos.responses.ProductResponse;
import application.adapters.rest.dtos.responses.WarehouseResponse;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.ProductRestMapper;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.InventoryItem;
import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.models.Seller;
import application.domain.ports.in.SellerPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.WarehouseId;
import application.infrastructure.security.ActorResolver;
import application.infrastructure.security.DomainUserPrincipal;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller endpoints: own catalogue, own warehouses and own stock.
 *
 * <p>The controller holds no output port: the identifiers the URL carries are resolved through the
 * entry port itself ({@code consultMyProduct}, {@code consultStockRecord}), which validates role and
 * ownership while loading (RG-03, RD-CAT-01). A seller asking for someone else's product is
 * therefore rejected by the resolution, not after it.</p>
 */
@RestController
@RequestMapping("/api/v1/seller")
public class SellerRestController {

    private final SellerPort sellerPort;

    public SellerRestController(SellerPort sellerPort) {
        this.sellerPort = Objects.requireNonNull(sellerPort);
    }

    // Catalogue

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> registerProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                           @Valid @RequestBody RegisterProductRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        Product product = ProductRestMapper.toDomain(request, seller.getIdentifier());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductRestMapper.toResponse(sellerPort.registerProduct(seller, product)));
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponse>> consultMyCatalog(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(PageResponse.of(sellerPort.consultMyCatalog(seller).stream()
                .map(ProductRestMapper::toResponse)
                .toList(), page, size));
    }

    /**
     * One product of the seller's own catalogue, in any status. A product of another seller answers
     * 403 and an unknown one 404 (RD-CAT-01).
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> consultMyProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                            @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(ProductRestMapper.toResponse(productOf(seller, productId)));
    }

    @PostMapping("/products/{productId}/publication")
    public ResponseEntity<ProductResponse> publishProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                          @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.publishProduct(seller, productOf(seller, productId))));
    }

    @DeleteMapping("/products/{productId}/publication")
    public ResponseEntity<ProductResponse> suspendProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                          @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.suspendProduct(seller, productOf(seller, productId))));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> discontinueProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                              @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.discontinueProduct(seller, productOf(seller, productId))));
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ProductResponse> addVariant(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                      @PathVariable String productId,
                                                      @Valid @RequestBody AddProductVariantRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        Product product = productOf(seller, productId);
        ProductVariant variant = ProductRestMapper.toVariantDomain(request, product.getIdentifier());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductRestMapper.toResponse(sellerPort.addProductVariant(seller, product, variant)));
    }

    @DeleteMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ProductResponse> removeVariant(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                         @PathVariable String productId,
                                                         @PathVariable String variantId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        Product product = productOf(seller, productId);
        ProductVariant variant = product.getVariants().stream()
                .filter(candidate -> candidate.getIdentifier().getValue().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "variant " + variantId + " does not belong to product " + productId));
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.removeProductVariant(seller, product, variant)));
    }

    // Warehouses

    @GetMapping("/warehouses")
    public ResponseEntity<PageResponse<WarehouseResponse>> consultMyWarehouses(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(PageResponse.of(sellerPort.consultMyWarehouses(seller).stream()
                .map(InventoryRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/warehouses/{warehouseId}")
    public ResponseEntity<WarehouseResponse> consultMyWarehouse(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String warehouseId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(InventoryRestMapper.toResponse(
                sellerPort.consultMyWarehouse(seller, WarehouseId.of(warehouseId))));
    }

    // Inventory

    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<PageResponse<InventoryItemResponse>> consultStockOfMyProduct(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(PageResponse.of(
                sellerPort.consultStockOfMyProduct(seller, ProductId.of(productId)).stream()
                        .map(InventoryRestMapper::toResponse)
                        .toList(), page, size));
    }

    /**
     * Opens the stock record of an own physical product in an own warehouse. Product and warehouse
     * are resolved through the port, which validates ownership while loading (RD-PIN-06): 404 when
     * either does not exist, 403 when either belongs to someone else.
     */
    @PostMapping("/stock/items")
    public ResponseEntity<InventoryItemResponse> openStockRecord(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody OpenStockRecordRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        InventoryItem inventoryItem = sellerPort.openStockRecord(
                seller,
                productOf(seller, request.productId()),
                sellerPort.consultMyWarehouse(seller, WarehouseId.of(request.warehouseId())),
                InventoryItemId.of(request.inventoryItemId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryRestMapper.toResponse(inventoryItem));
    }

    @PostMapping("/stock/receipts")
    public ResponseEntity<InventoryMovementResponse> receiveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        InventoryMovementResponse response = InventoryRestMapper.toResponse(sellerPort.receiveStock(
                seller, stockOf(seller, request.inventoryItemId()), Quantity.of(request.quantity()),
                InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/adjustments")
    public ResponseEntity<InventoryMovementResponse> adjustStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        InventoryMovementResponse response = InventoryRestMapper.toResponse(sellerPort.adjustStock(
                seller, stockOf(seller, request.inventoryItemId()), Quantity.of(request.quantity()),
                InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/{inventoryItemId}/damage")
    public ResponseEntity<InventoryItemResponse> markStockAsDamaged(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String inventoryItemId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(InventoryRestMapper.toResponse(
                sellerPort.markStockAsDamaged(seller, stockOf(seller, inventoryItemId))));
    }

    private Product productOf(Seller seller, String productId) {
        return sellerPort.consultMyProduct(seller, ProductId.of(productId));
    }

    private InventoryItem stockOf(Seller seller, String inventoryItemId) {
        return sellerPort.consultStockRecord(seller, InventoryItemId.of(inventoryItemId));
    }
}
