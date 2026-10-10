package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.domain.models.DigitalProduct;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import application.domain.models.ProductVariant;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductStatus;
import application.domain.valuesObjects.ProductType;
import application.domain.valuesObjects.ProductVariantId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.VariantAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates a product between its domain shape and its persistence shape.
 *
 * <p>{@code productType} decides the specialization rebuilt, and both are restored through their
 * {@code reconstruct} factory so the stored catalogue status and variants survive the round trip
 * instead of being reset to the ones of a brand new product (RD-ALC-04, RD-CAT-02).</p>
 */
public final class ProductJpaMapper {

    private ProductJpaMapper() {
    }

    public static ProductJpaEntity toEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getIdentifier().getValue());
        entity.setSellerId(product.getSellerId().getValue());
        entity.setProductType(product.getProductType().code());
        entity.setStatus(product.getStatus().code());
        product.getVariants().forEach(variant -> entity.getVariants().add(toVariantEntity(variant, entity)));
        return entity;
    }

    public static Product toDomain(ProductJpaEntity entity) {
        List<ProductVariant> variants = entity.getVariants().stream()
                .map(ProductJpaMapper::toVariantDomain)
                .toList();
        ProductId identifier = ProductId.of(entity.getId());
        SellerId sellerId = SellerId.of(entity.getSellerId());
        ProductStatus status = ProductStatus.valueOf(entity.getStatus());
        return ProductType.valueOf(entity.getProductType()) == ProductType.PHYSICAL
                ? PhysicalProduct.reconstruct(identifier, sellerId, status, variants)
                : DigitalProduct.reconstruct(identifier, sellerId, status, variants);
    }

    private static ProductVariantJpaEntity toVariantEntity(ProductVariant variant, ProductJpaEntity product) {
        ProductVariantJpaEntity entity = new ProductVariantJpaEntity();
        entity.setId(variant.getIdentifier().getValue());
        entity.setProduct(product);
        Map<String, String> attributes = new LinkedHashMap<>();
        variant.getAttributes().forEach(attribute -> attributes.put(attribute.getName(), attribute.getValue()));
        entity.setAttributes(attributes);
        return entity;
    }

    private static ProductVariant toVariantDomain(ProductVariantJpaEntity entity) {
        List<VariantAttribute> attributes = entity.getAttributes().entrySet().stream()
                .map(attribute -> VariantAttribute.of(attribute.getKey(), attribute.getValue()))
                .toList();
        return new ProductVariant(
                ProductVariantId.of(entity.getId()),
                ProductId.of(entity.getProduct().getId()),
                attributes);
    }
}
