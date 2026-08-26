package application.domain.models;

import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.WarehouseId;
import application.domain.valuesObjects.WarehouseType;
import java.util.Objects;
import lombok.Getter;

/**
 * Warehouse owned by a seller. It is registered together with the incorporation of the seller by
 * the administrator (RD-ROL-05).
 */
@Getter
public class SellerWarehouse extends Warehouse {

    private final SellerId sellerId;

    public SellerWarehouse(WarehouseId identifier, SellerId sellerId) {
        super(identifier, WarehouseType.SELLER);
        this.sellerId = Objects.requireNonNull(sellerId, "owning seller is mandatory");
    }

    /**
     * Supports RG-03: the warehouse is only managed by the seller who owns it.
     */
    public boolean belongsTo(Seller seller) {
        return seller != null && sellerId.equals(seller.getIdentifier());
    }

    /**
     * Ownership check against the identifier alone, usable before the seller entity is available.
     */
    public boolean belongsTo(SellerId candidate) {
        return sellerId.equals(candidate);
    }
}
