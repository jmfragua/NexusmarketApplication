package application.domain.services.user;

import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.models.Buyer;
import application.domain.ports.out.UserRepositoryPort;
import java.util.Objects;

/**
 * Incorporates a buyer to the platform.
 *
 * <p>The email address and the identity document are unique across the whole platform. That
 * uniqueness depends on the data set and cannot be checked inside the value object (RD-VO-09), so
 * it is verified here, when the user joins the domain (RD-ID-03, RD-ID-04).</p>
 */
public class RegisterBuyerService {

    private final UserRepositoryPort userRepositoryPort;

    public RegisterBuyerService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "the user repository is mandatory");
    }

    /**
     * @param buyer buyer already built and validated by the domain model.
     * @return the registered buyer.
     * @throws DuplicateEmailException            when the email address is already registered.
     * @throws DuplicateIdentityDocumentException when the identity document is already registered.
     */
    public Buyer register(Buyer buyer) {
        Objects.requireNonNull(buyer, "buyer is mandatory");
        if (userRepositoryPort.existsByEmail(buyer.getEmail())) {
            throw new DuplicateEmailException(buyer.getEmail());
        }
        if (userRepositoryPort.existsByIdentityDocument(buyer.getIdentityDocument())) {
            throw new DuplicateIdentityDocumentException(buyer.getIdentityDocument());
        }
        return userRepositoryPort.save(buyer);
    }
}
