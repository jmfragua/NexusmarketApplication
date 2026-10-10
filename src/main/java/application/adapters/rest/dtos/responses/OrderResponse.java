package application.adapters.rest.dtos.responses;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outbound shape of an order.
 *
 * <p>{@code modifiable} is exposed because a finished order can never be modified (RD-PED-02) and
 * that fact drives what a client may offer next.</p>
 */
public record OrderResponse(String id,
                            String buyerId,
                            String status,
                            boolean modifiable,
                            String deliveryAddress,
                            BigDecimal totalAmount,
                            String currency,
                            List<OrderLineResponse> lines) {
}
