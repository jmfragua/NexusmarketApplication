package application.domain.services.seller;

import application.domain.exceptions.EntityNotFoundException;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserRole;
import java.util.List;
import java.util.Objects;

/**
 * Consults the sellers of the platform. Administration and monitoring of the sellers belong to the
 * administrator and to the supervisor, the read only monitoring profile (RD-ROL-06).
 */
public class ConsultSellerService {

    private static final String OPERATION = "consult seller";

    private final SellerRepositoryPort sellerRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ConsultSellerService(SellerRepositoryPort sellerRepositoryPort,
                                ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.sellerRepositoryPort = Objects.requireNonNull(sellerRepositoryPort, "the seller repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @throws EntityNotFoundException when no seller holds that identifier.
     */
    public Seller consult(User<?> actor, SellerId sellerId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return sellerRepositoryPort.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("seller", sellerId));
    }

    public List<Seller> consultAll(User<?> actor) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR, UserRole.SUPERVISOR);
        return sellerRepositoryPort.findAll();
    }
}
