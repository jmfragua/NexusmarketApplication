package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a seller.
 */
public record SellerResponse(String id,
                             String fullName,
                             String email,
                             String identityDocument,
                             String status) {
}
