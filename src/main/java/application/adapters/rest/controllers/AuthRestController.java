package application.adapters.rest.controllers;

import application.adapters.rest.dtos.requests.LoginRequest;
import application.adapters.rest.dtos.requests.RegisterBuyerRequest;
import application.adapters.rest.dtos.responses.AuthResponse;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.in.PublicAccessPort;
import application.domain.valuesObjects.Address;
import application.domain.valuesObjects.BuyerCommercialStatus;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.EmailAddress;
import application.domain.valuesObjects.FullName;
import application.domain.valuesObjects.IdentityDocument;
import application.domain.valuesObjects.UserStatus;
import application.infrastructure.security.CredentialService;
import application.infrastructure.security.JwtProvider;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Open to unauthenticated callers.
 *
 * <p>Only buyers self register: sellers are incorporated by the administrator along with their
 * first warehouse (RD-ROL-05), so there is no seller registration here.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final PublicAccessPort publicAccessPort;

    private final CredentialService credentialService;

    private final JwtProvider jwtProvider;

    public AuthRestController(PublicAccessPort publicAccessPort,
                              CredentialService credentialService,
                              JwtProvider jwtProvider) {
        this.publicAccessPort = Objects.requireNonNull(publicAccessPort);
        this.credentialService = Objects.requireNonNull(credentialService);
        this.jwtProvider = Objects.requireNonNull(jwtProvider);
    }

    /**
     * Registers a buyer and returns a token, so the caller does not need a second round trip to
     * start operating.
     *
     * <p>The credential is stored only after the domain accepted the buyer: a rejected registration
     * — a duplicated email, for instance — must not leave an orphan password behind.</p>
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterBuyerRequest request) {
        Buyer buyer = new Buyer(
                BuyerId.of(request.buyerId()),
                FullName.of(request.fullName()),
                EmailAddress.of(request.email()),
                IdentityDocument.of(request.identityDocument()),
                UserStatus.ACTIVE,
                Address.of(request.primaryAddress()),
                BuyerCommercialStatus.ENABLED);
        Buyer registered = publicAccessPort.registerBuyer(buyer, CartId.of(UUID.randomUUID().toString()));
        credentialService.register(registered.getEmail(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponseOf(registered));
    }

    /**
     * @throws UnauthorizedOperationException when the password does not match. The domain resolves
     *         <em>who</em> the user is and whether they may operate (RG-01); verifying the secret is
     *         the responsibility of this adapter (RD-ALC-01).
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        EmailAddress email = EmailAddress.of(request.email());
        if (!credentialService.matches(email, request.password())) {
            throw new UnauthorizedOperationException("invalid credentials");
        }
        User<?> user = publicAccessPort.login(email);
        return ResponseEntity.ok(authResponseOf(user));
    }

    private AuthResponse authResponseOf(User<?> user) {
        return new AuthResponse(
                jwtProvider.generateToken(user),
                user.getIdentifier().getValue(),
                user.getEmail().getValue(),
                user.getRole().code());
    }
}
