package application.domain.services.inventory;

import application.domain.exceptions.DamagedStockException;
import application.domain.exceptions.InsufficientStockException;
import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.OrderLine;
import application.domain.models.User;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.services.authorization.ValidateRoleAuthorizationService;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.UserRole;
import java.util.Objects;

/**
 * Commits the stock of an order line, which produces a reservation movement.
 *
 * <p>Stock that does not exist or is marked as damaged can never be reserved (RD-INV-03), and no
 * operation may leave the balance in negative (RD-INV-02).</p>
 */
public class ReserveStockService {

    private static final String OPERATION = "reserve stock";

    private final InventoryItemRepositoryPort inventoryItemRepositoryPort;

    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;

    private final ValidateRoleAuthorizationService validateRoleAuthorizationService;

    public ReserveStockService(InventoryItemRepositoryPort inventoryItemRepositoryPort,
                               InventoryMovementRepositoryPort inventoryMovementRepositoryPort,
                               ValidateRoleAuthorizationService validateRoleAuthorizationService) {
        this.inventoryItemRepositoryPort = Objects.requireNonNull(inventoryItemRepositoryPort,
                "the inventory repository is mandatory");
        this.inventoryMovementRepositoryPort = Objects.requireNonNull(inventoryMovementRepositoryPort,
                "the movement repository is mandatory");
        this.validateRoleAuthorizationService = Objects.requireNonNull(validateRoleAuthorizationService,
                "the role validation is mandatory");
    }

    /**
     * @param orderLine line of the order committing the stock.
     * @return the reservation movement recorded in the traceability of the stock.
     * @throws DamagedStockException       when the stock is marked as damaged (RD-INV-03).
     * @throws InsufficientStockException  when the available stock does not cover the line.
     */
    public InventoryMovement reserve(User<?> actor,
                                     OrderLine orderLine,
                                     InventoryItem inventoryItem,
                                     InventoryMovementId movementId) {
        validateRoleAuthorizationService.validate(actor, OPERATION, UserRole.SELLER, UserRole.LOGISTICS_OPERATOR);
        Objects.requireNonNull(orderLine, "order line is mandatory");
        Objects.requireNonNull(inventoryItem, "the stock record is mandatory");
        if (!inventoryItem.getCondition().allowsReservation()) {
            throw new DamagedStockException(inventoryItem.getProductId());
        }
        if (!inventoryItem.canReserve(orderLine.getQuantity())) {
            throw new InsufficientStockException(inventoryItem.getProductId(),
                    inventoryItem.getAvailableQuantity(), orderLine.getQuantity());
        }
        InventoryMovement movement = orderLine.reserveStock(inventoryItem, movementId);
        inventoryItemRepositoryPort.save(inventoryItem);
        return inventoryMovementRepositoryPort.save(movement);
    }
}
