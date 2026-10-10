package application.adapters.rest.dtos.responses;

import java.util.List;

/**
 * Outbound shape of a buyer.
 */
public record BuyerResponse(String id,
                            String fullName,
                            String email,
                            String identityDocument,
                            String status,
                            String primaryAddress,
                            List<String> additionalAddresses,
                            String commercialStatus) {
}
