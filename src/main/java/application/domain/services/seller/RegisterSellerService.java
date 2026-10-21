package application.domain.services.seller;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentifierException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.DuplicateUserIdentifierException;
import application.domain.models.Seller;
import application.domain.models.SellerWarehouse;
import application.domain.models.User;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.services.audit.RegisterOperationLogService;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.OperationType;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;
import java.util.Objects;

/**
 * Incorporates a seller together with their first warehouse.
 *
 * <p>Sellers cannot register themselves: the creation of a seller is an operation reserved to the
 * administrator and is always performed along with their first warehouse (RD-ROL-05, RD-ROL-06).
 * The email address and the identity document stay unique across the platform (RD-ID-03,
 * RD-ID-04).</p>
 */
public class RegisterSellerService {

    private static final String OPERATION = "register seller";

    private final SellerRepositoryPort sellerRepositoryPort;

    private final WarehouseRepositoryPort warehouseRepositoryPort;

    private final UserRepositoryPort userRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    private final RegisterOperationLogService registerOperationLogService;

    public RegisterSellerService(SellerRepositoryPort sellerRepositoryPort,
                                 WarehouseRepositoryPort warehouseRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 ValidateRoleAuthorizationService validateRoleAuthorizationService,
                                 RegisterOperationLogService registerOperationLogService) {
        this.sellerRepositoryPort = Objects.requireNonNull(sellerRepositoryPort, "the seller repository is mandatory");
        this.warehouseRepositoryPort = Objects.requireNonNull(warehouseRepositoryPort,
                "the warehouse repository is mandatory");
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
        this.registerOperationLogService = Objects.requireNonNull(registerOperationLogService,
                "the operation log registration is mandatory");
    }

    /**
     * @param actor          administrator incorporating the seller.
     * @param firstWarehouse first warehouse of the seller, registered along with them.
     * @return the incorporated seller.
     * @throws DuplicateUserIdentifierException   when another user already holds the identifier (RD-ID-02).
     * @throws DuplicateEmailException            when the email address is already registered.
     * @throws DuplicateIdentityDocumentException when the identity document is already registered.
     */
    public Seller register(User<?> actor,
                           SellerId sellerId,
                           FullName fullName,
                           EmailAddress email,
                           IdentityDocument identityDocument,
                           UserStatus status,
                           SellerWarehouse firstWarehouse) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.ADMINISTRATOR);
        if (userRepositoryPort.findByIdentifier(sellerId).isPresent()) {
            throw new DuplicateUserIdentifierException(sellerId);
        }
        Objects.requireNonNull(firstWarehouse, "the first warehouse is mandatory");
        if (warehouseRepositoryPort.findById(firstWarehouse.getIdentifier()).isPresent()) {
            throw new DuplicateIdentifierException("warehouse", firstWarehouse.getIdentifier());
        }
        if (userRepositoryPort.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        if (userRepositoryPort.existsByIdentityDocument(identityDocument)) {
            throw new DuplicateIdentityDocumentException(identityDocument);
        }
        Seller seller = Seller.register(sellerId, fullName, email, identityDocument, status, firstWarehouse, actor);
        warehouseRepositoryPort.create(firstWarehouse);
        Seller registeredSeller = sellerRepositoryPort.create(seller);
        registerOperationLogService.register(actor, OperationType.SELLER_REGISTERED, registeredSeller,
                "seller incorporated with first warehouse " + firstWarehouse.getIdentifier());
        return registeredSeller;
    }
}
