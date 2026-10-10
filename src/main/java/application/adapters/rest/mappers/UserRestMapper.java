package application.adapters.rest.mappers;

import application.adapters.rest.dtos.responses.BuyerResponse;
import application.adapters.rest.dtos.responses.SellerResponse;
import application.adapters.rest.dtos.responses.UserResponse;
import application.domain.models.Buyer;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.valuesObjects.Address;

/**
 * Translates users between their domain shape and the REST shapes.
 */
public final class UserRestMapper {

    private UserRestMapper() {
    }

    public static UserResponse toResponse(User<?> user) {
        return new UserResponse(
                user.getIdentifier().getValue(),
                user.getFullName().getValue(),
                user.getEmail().getValue(),
                user.getIdentityDocument().getValue(),
                user.getRole().code(),
                user.getStatus().code());
    }

    public static BuyerResponse toResponse(Buyer buyer) {
        return new BuyerResponse(
                buyer.getIdentifier().getValue(),
                buyer.getFullName().getValue(),
                buyer.getEmail().getValue(),
                buyer.getIdentityDocument().getValue(),
                buyer.getStatus().code(),
                buyer.getPrimaryAddress().getValue(),
                buyer.getAdditionalAddresses().stream().map(Address::getValue).toList(),
                buyer.getCommercialStatus().code());
    }

    public static SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getIdentifier().getValue(),
                seller.getFullName().getValue(),
                seller.getEmail().getValue(),
                seller.getIdentityDocument().getValue(),
                seller.getStatus().code());
    }
}
