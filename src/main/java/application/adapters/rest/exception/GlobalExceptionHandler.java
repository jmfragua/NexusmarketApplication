package application.adapters.rest.exception;

import application.adapters.rest.dtos.responses.ErrorResponse;
import application.domain.exceptions.BuyerNotAllowedToPurchaseException;
import application.domain.exceptions.DamagedStockException;
import application.domain.exceptions.DomainException;
import application.domain.exceptions.DuplicateEmailException;
import application.domain.exceptions.DuplicateIdentityDocumentException;
import application.domain.exceptions.DuplicateInventoryItemException;
import application.domain.exceptions.DuplicateUserIdentifierException;
import application.domain.exceptions.EmptyCartException;
import application.domain.exceptions.EntityNotFoundException;
import application.domain.exceptions.InsufficientStockException;
import application.domain.exceptions.InvalidRoleChangeException;
import application.domain.exceptions.InvalidOrderTransitionException;
import application.domain.exceptions.InvoiceNotIssuedException;
import application.domain.exceptions.OrderNotModifiableException;
import application.domain.exceptions.ProductNotAvailableException;
import application.domain.exceptions.ProductNotOwnedException;
import application.domain.exceptions.RefundExceedsInvoiceException;
import application.domain.exceptions.StaffRoleNotAllowedException;
import application.domain.exceptions.StockRecordNotAllowedException;
import application.domain.exceptions.ReturnNotAllowedException;
import application.domain.exceptions.UnauthorizedOperationException;
import application.domain.exceptions.UserNotActiveException;
import application.domain.exceptions.WarehouseNotOwnedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates the domain exceptions into HTTP responses.
 *
 * <p>This is the only place where a business rule becomes a status code. The domain never knows
 * about HTTP, and controllers never catch exceptions: they let them travel up to here, which keeps
 * the mapping in one table instead of scattered across endpoints.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Status per rule. The grouping is deliberate:
     * <ul>
     *   <li><b>403</b> — the actor may not do this (RG-01, RG-03 and the ownership rules).</li>
     *   <li><b>404</b> — the referenced entity does not exist.</li>
     *   <li><b>409</b> — the entity exists but its current state forbids the operation.</li>
     *   <li><b>400</b> — the request itself is inconsistent with the business rules.</li>
     * </ul>
     */
    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_RULE = Map.ofEntries(
            Map.entry(UnauthorizedOperationException.class, HttpStatus.FORBIDDEN),
            Map.entry(UserNotActiveException.class, HttpStatus.FORBIDDEN),
            Map.entry(ProductNotOwnedException.class, HttpStatus.FORBIDDEN),
            Map.entry(WarehouseNotOwnedException.class, HttpStatus.FORBIDDEN),
            Map.entry(BuyerNotAllowedToPurchaseException.class, HttpStatus.FORBIDDEN),
            Map.entry(EntityNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(InvoiceNotIssuedException.class, HttpStatus.NOT_FOUND),
            Map.entry(OrderNotModifiableException.class, HttpStatus.CONFLICT),
            Map.entry(InvalidOrderTransitionException.class, HttpStatus.CONFLICT),
            Map.entry(InsufficientStockException.class, HttpStatus.CONFLICT),
            Map.entry(DamagedStockException.class, HttpStatus.CONFLICT),
            Map.entry(ProductNotAvailableException.class, HttpStatus.CONFLICT),
            Map.entry(ReturnNotAllowedException.class, HttpStatus.CONFLICT),
            Map.entry(EmptyCartException.class, HttpStatus.CONFLICT),
            Map.entry(DuplicateEmailException.class, HttpStatus.CONFLICT),
            Map.entry(DuplicateIdentityDocumentException.class, HttpStatus.CONFLICT),
            Map.entry(DuplicateUserIdentifierException.class, HttpStatus.CONFLICT),
            Map.entry(DuplicateInventoryItemException.class, HttpStatus.CONFLICT),
            Map.entry(StockRecordNotAllowedException.class, HttpStatus.CONFLICT),
            Map.entry(InvalidRoleChangeException.class, HttpStatus.CONFLICT),
            Map.entry(RefundExceedsInvoiceException.class, HttpStatus.BAD_REQUEST),
            Map.entry(StaffRoleNotAllowedException.class, HttpStatus.BAD_REQUEST));

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
        HttpStatus status = STATUS_BY_RULE.getOrDefault(exception.getClass(), HttpStatus.BAD_REQUEST);
        return build(status, exception.getClass().getSimpleName(), exception.getMessage());
    }

    /**
     * Invariants guarded by the model itself rather than by a named rule — a transition the entity
     * refuses, for instance — surface as a conflict: the request was well formed, the state was not.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        return build(HttpStatus.CONFLICT, "DomainInvariant", exception.getMessage());
    }

    /**
     * Value object guards reject malformed input before any rule is reached.
     */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(RuntimeException exception) {
        return build(HttpStatus.BAD_REQUEST, "InvalidInput", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .reduce((first, second) -> first + "; " + second)
                .orElse("the request is not valid");
        return build(HttpStatus.BAD_REQUEST, "RequestValidation", message);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String rule, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), rule, message));
    }
}
