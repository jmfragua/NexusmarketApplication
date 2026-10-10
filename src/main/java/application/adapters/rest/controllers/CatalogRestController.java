package application.adapters.rest.controllers;

import application.adapters.rest.dtos.responses.PageResponse;
import application.adapters.rest.dtos.responses.ProductResponse;
import application.adapters.rest.mappers.ProductRestMapper;
import application.domain.ports.in.PublicAccessPort;
import application.domain.valuesObjects.ProductId;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalogue endpoints.
 *
 * <p>Deliberately open to unauthenticated callers: the catalogue is the public face of the
 * marketplace and a visitor must be able to browse it before registering. It is the only read that
 * steps outside RG-01, and it exposes exclusively published products holding at least one variant
 * (RD-CAT-03, RD-CAT-05).</p>
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogRestController {

    private final PublicAccessPort publicAccessPort;

    public CatalogRestController(PublicAccessPort publicAccessPort) {
        this.publicAccessPort = Objects.requireNonNull(publicAccessPort);
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponse>> consultPublicCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(publicAccessPort.consultPublicCatalog().stream()
                .map(ProductRestMapper::toResponse)
                .toList(), page, size));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> consultPublishedProduct(@PathVariable String productId) {
        return ResponseEntity.ok(
                ProductRestMapper.toResponse(publicAccessPort.consultPublishedProduct(ProductId.of(productId))));
    }
}
