package application.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Answers the rejections of the security filter chain with the same {@code {status, rule, message}}
 * body the domain errors use, so a client can tell them apart from a bare server failure.
 *
 * <p>Without it Spring answers an anonymous request with an empty 403, which a client cannot
 * distinguish from a role violation: an expired session would never be detected as such. Here a
 * request without a valid authentication gets 401, and an authenticated user whose role does not
 * reach the route gets 403.</p>
 */
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String AUTHENTICATION_REQUIRED = "AuthenticationRequired";

    static final String ACCESS_DENIED = "AccessDenied";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED,
                "authentication is required: the bearer token is missing, invalid or expired, or its user is no longer active");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ACCESS_DENIED,
                "the role of the authenticated user does not allow this operation");
    }

    private static void write(HttpServletResponse response, HttpStatus status, String rule, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The values are fixed constants of this class, so plain formatting cannot produce invalid JSON.
        response.getWriter().write(
                "{\"status\":" + status.value() + ",\"rule\":\"" + rule + "\",\"message\":\"" + message + "\"}");
    }
}
