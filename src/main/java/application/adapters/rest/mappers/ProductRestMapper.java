package application.adapters.rest.mappers;

import application.adapters.rest.dtos.requests.AddProductVariantRequest;
import application.adapters.rest.dtos.requests.RegisterProductRequest;
import application.adapters.rest.dtos.responses.ProductResponse;
import application.adapters.rest.dtos.responses.ProductVariantResponse;
import application.domain.models.DigitalProduct;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.VariantAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates products between their domain shape and the REST shapes.
 */
public final class ProductRestMapper {

    private ProductRestMapper() {
    }

    /**
     * Builds a brand new product of the requested type. The type is decided here, once, and never
     * changes afterwards (RD-CAT-02).
     */
    public static Product toDomain(RegisterProductRequest request, SellerId sellerId) {
        ProductId productId = ProductId.of(request.productId());
        return ProductType.valueOf(request.productType()) == ProductType.PHYSICAL
                ? new PhysicalProduct(productId, sellerId)
                : new DigitalProduct(productId, sellerId);
    }

    public static ProductVariant toVariantDomain(AddProductVariantRequest request, ProductId productId) {
        List<VariantAttribute> attributes = request.attributes().entrySet().stream()
                .map(attribute -> VariantAttribute.of(attribute.getKey(), attribute.getValue()))
                .toList();
        return new ProductVariant(ProductVariantId.of(request.variantId()), productId, attributes);
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getIdentifier().getValue(),
                product.getSellerId().getValue(),
                product.getProductType().code(),
                product.getStatus().code(),
                product.isVisibleInCatalog(),
                product.getVariants().stream().map(ProductRestMapper::toResponse).toList());
    }

    public static ProductVariantResponse toResponse(ProductVariant variant) {
        Map<String, String> attributes = new LinkedHashMap<>();
        variant.getAttributes().forEach(attribute -> attributes.put(attribute.getName(), attribute.getValue()));
        return new ProductVariantResponse(variant.getIdentifier().getValue(), attributes);
    }
}
