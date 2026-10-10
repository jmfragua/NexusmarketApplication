package application.adapters.rest.dtos.responses;

/**
 * Outbound shape of a rejected request.
 *
 * <p>{@code rule} names the domain exception that rejected it, which keeps the business rule
 * traceable from the HTTP response back to the model.</p>
 */
public record ErrorResponse(int status, String rule, String message) {
}
