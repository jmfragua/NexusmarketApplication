package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.AddProductVariantRequest;
import application.adapters.rest.dtos.requests.RegisterProductRequest;
import application.adapters.rest.dtos.requests.StockMovementRequest;
import application.adapters.rest.dtos.responses.InventoryItemResponse;
import application.adapters.rest.dtos.responses.InventoryMovementResponse;
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
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.Quantity;
import application.domain.valuesObjects.WarehouseId;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller endpoints: own catalogue, own warehouses and own stock.
 *
 * <p>Output ports are injected here only to <em>resolve</em> an entity from the identifier the URL
 * carries. They never decide anything: every operation afterwards goes through the entry port, and
 * the domain service re-validates role and ownership before touching the model (RG-03, RD-CAT-01).
 * Loading through a repository therefore grants no privilege — a seller who resolves someone else's
 * product still gets rejected by {@code ValidateSellerOwnershipService}.</p>
 */
@RestController
@RequestMapping("/api/v1/seller")
public class SellerRestController {

    private final SellerPort sellerPort;

    private final ProductRepositoryPort productRepositoryPort;

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    public SellerRestController(SellerPort sellerPort,
                                ProductRepositoryPort productRepositoryPort,
                                InventoryItemRepositoryPort inventoryItemRepositoryPort) {
        this.sellerPort = Objects.requireNonNull(sellerPort);
        this.productRepositoryPort = Objects.requireNonNull(productRepositoryPort);
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort);
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
    public ResponseEntity<List<ProductResponse>> consultMyCatalog(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(sellerPort.consultMyCatalog(seller).stream()
                .map(ProductRestMapper::toResponse)
                .toList());
    }

    @PostMapping("/products/{productId}/publication")
    public ResponseEntity<ProductResponse> publishProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                          @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.publishProduct(seller, productOf(productId))));
    }

    @DeleteMapping("/products/{productId}/publication")
    public ResponseEntity<ProductResponse> suspendProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                          @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.suspendProduct(seller, productOf(productId))));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> discontinueProduct(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                              @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(sellerPort.discontinueProduct(seller, productOf(productId))));
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ProductResponse> addVariant(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                      @PathVariable String productId,
                                                      @Valid @RequestBody AddProductVariantRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        Product product = productOf(productId);
        ProductVariant variant = ProductRestMapper.toVariantDomain(request, product.getIdentifier());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductRestMapper.toResponse(sellerPort.addProductVariant(seller, product, variant)));
    }

    @DeleteMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ProductResponse> removeVariant(@AuthenticationPrincipal DomainUserPrincipal principal,
                                                         @PathVariable String productId,
                                                         @PathVariable String variantId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        Product product = productOf(productId);
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
    public ResponseEntity<List<WarehouseResponse>> consultMyWarehouses(
            @AuthenticationPrincipal DomainUserPrincipal principal) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(sellerPort.consultMyWarehouses(seller).stream()
                .map(InventoryRestMapper::toResponse)
                .toList());
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
    public ResponseEntity<List<InventoryItemResponse>> consultStockOfMyProduct(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String productId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(sellerPort.consultStockOfMyProduct(seller, ProductId.of(productId)).stream()
                .map(InventoryRestMapper::toResponse)
                .toList());
    }

    @PostMapping("/stock/receipts")
    public ResponseEntity<InventoryMovementResponse> receiveStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        InventoryMovementResponse response = InventoryRestMapper.toResponse(sellerPort.receiveStock(
                seller, stockOf(request.inventoryItemId()), Quantity.of(request.quantity()),
                InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/adjustments")
    public ResponseEntity<InventoryMovementResponse> adjustStock(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @Valid @RequestBody StockMovementRequest request) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        InventoryMovementResponse response = InventoryRestMapper.toResponse(sellerPort.adjustStock(
                seller, stockOf(request.inventoryItemId()), Quantity.of(request.quantity()),
                InventoryMovementId.of(request.movementId())));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stock/{inventoryItemId}/damage")
    public ResponseEntity<InventoryItemResponse> markStockAsDamaged(
            @AuthenticationPrincipal DomainUserPrincipal principal,
            @PathVariable String inventoryItemId) {
        Seller seller = ActorResolver.requireSeller(principal.user());
        return ResponseEntity.ok(InventoryRestMapper.toResponse(
                sellerPort.markStockAsDamaged(seller, stockOf(inventoryItemId))));
    }

    private Product productOf(String productId) {
        return productRepositoryPort.findById(ProductId.of(productId))
                .orElseThrow(() -> new EntityNotFoundException("product", ProductId.of(productId)));
    }

    private InventoryItem stockOf(String inventoryItemId) {
        return inventoryItemRepositoryPort.findById(InventoryItemId.of(inventoryItemId))
                .orElseThrow(() -> new EntityNotFoundException("stock record",
                        InventoryItemId.of(inventoryItemId)));
    }
}
