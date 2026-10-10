package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.domain.models.Buyer;
import application.domain.models.Seller;
import application.domain.models.StaffUser;
import application.domain.models.User;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.UserRole;
import application.domain.valuesObjects.UserStatus;

/**
 * Translates a user between its domain shape and its persistence shape.
 *
 * <p>{@code userType} decides which specialization is rebuilt. A seller is restored through
 * {@link Seller#reconstruct}, because {@code Seller.register} protects the incorporation rules of a
 * brand new seller, not the reload of an existing one (RD-ALC-04, RD-ROL-05).</p>
 */
public final class UserJpaMapper {

    private static final String BUYER = "BUYER";

    private static final String SELLER = "SELLER";

    /**
     * Discriminator of the roles that own no specialization — logistics operator, administrator and
     * supervisor — all carried by {@code StaffUser}. Their actual role lives in the {@code role}
     * column, which is what tells them apart.
     */
    private static final String STAFF = "STAFF";

    private UserJpaMapper() {
    }

    public static UserJpaEntity toEntity(User<?> user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getIdentifier().getValue());
        entity.setFullName(user.getFullName().getValue());
        entity.setEmail(user.getEmail().getValue());
        entity.setIdentityDocument(user.getIdentityDocument().getValue());
        entity.setRole(user.getRole().code());
        entity.setStatus(user.getStatus().code());
        if (user instanceof Buyer buyer) {
            entity.setUserType(BUYER);
            entity.setPrimaryAddress(buyer.getPrimaryAddress().getValue());
            entity.setCommercialStatus(buyer.getCommercialStatus().code());
            buyer.getAdditionalAddresses().forEach(address -> entity.getAdditionalAddresses().add(address.getValue()));
        } else if (user instanceof Seller) {
            entity.setUserType(SELLER);
        } else {
            entity.setUserType(STAFF);
        }
        return entity;
    }

    public static User<?> toDomain(UserJpaEntity entity) {
        if (BUYER.equals(entity.getUserType())) {
            return toBuyerDomain(entity);
        }
        if (SELLER.equals(entity.getUserType())) {
            return toSellerDomain(entity);
        }
        if (STAFF.equals(entity.getUserType())) {
            return toStaffDomain(entity);
        }
        throw new IllegalStateException("unsupported stored user type: " + entity.getUserType());
    }

    public static StaffUser toStaffDomain(UserJpaEntity entity) {
        return StaffUser.of(
                UserId.of(entity.getId()),
                FullName.of(entity.getFullName()),
                EmailAddress.of(entity.getEmail()),
                IdentityDocument.of(entity.getIdentityDocument()),
                UserRole.valueOf(entity.getRole()),
                UserStatus.valueOf(entity.getStatus()));
    }

    public static Buyer toBuyerDomain(UserJpaEntity entity) {
        Buyer buyer = new Buyer(
                BuyerId.of(entity.getId()),
                FullName.of(entity.getFullName()),
                EmailAddress.of(entity.getEmail()),
                IdentityDocument.of(entity.getIdentityDocument()),
                UserStatus.valueOf(entity.getStatus()),
                Address.of(entity.getPrimaryAddress()),
                BuyerCommercialStatus.valueOf(entity.getCommercialStatus()));
        entity.getAdditionalAddresses().forEach(address -> buyer.addAdditionalAddress(Address.of(address)));
        return buyer;
    }

    public static Seller toSellerDomain(UserJpaEntity entity) {
        return Seller.reconstruct(
                SellerId.of(entity.getId()),
                FullName.of(entity.getFullName()),
                EmailAddress.of(entity.getEmail()),
                IdentityDocument.of(entity.getIdentityDocument()),
                UserStatus.valueOf(entity.getStatus()));
    }

    public static boolean isBuyer(UserJpaEntity entity) {
        return BUYER.equals(entity.getUserType());
    }

    public static boolean isSeller(UserJpaEntity entity) {
        return SELLER.equals(entity.getUserType());
    }

    public static UserRole roleOf(UserJpaEntity entity) {
        return UserRole.valueOf(entity.getRole());
    }
}
