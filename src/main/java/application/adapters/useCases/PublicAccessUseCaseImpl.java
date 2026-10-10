package application.adapters.useCases;

import application.domain.models.Buyer;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.in.PublicAccessPort;
import application.domain.services.catalog.ConsultCatalogService;
import application.domain.services.user.LoginService;
import application.domain.services.user.RegisterBuyerService;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.ProductId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Implements the public entry port by delegating to the domain services.
 *
 * <p>A use case holds no business rule of its own: it is the seam where the framework meets the
 * domain, and every method is a one line delegation to the service that owns the rule.</p>
 */
@Service
public class PublicAccessUseCaseImpl implements PublicAccessPort {

    private final LoginService loginService;

    private final RegisterBuyerService registerBuyerService;

    private final ConsultCatalogService consultCatalogService;

    public PublicAccessUseCaseImpl(LoginService loginService,
                                   RegisterBuyerService registerBuyerService,
                                   ConsultCatalogService consultCatalogService) {
        this.loginService = Objects.requireNonNull(loginService, "the login service is mandatory");
        this.registerBuyerService = Objects.requireNonNull(registerBuyerService,
                "the buyer registration service is mandatory");
        this.consultCatalogService = Objects.requireNonNull(consultCatalogService,
                "the catalogue consultation service is mandatory");
    }

    @Override
    public User<?> login(EmailAddress email) {
        return loginService.login(email);
    }

    @Override
    public Buyer registerBuyer(Buyer buyer, CartId cartId) {
        return registerBuyerService.register(buyer, cartId);
    }

    @Override
    public List<Product> consultPublicCatalog() {
        return consultCatalogService.consultPublicCatalog();
    }

    @Override
    public Product consultPublishedProduct(ProductId productId) {
        return consultCatalogService.consultPublished(productId);
    }
}
