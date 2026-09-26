package application.domain.ports.in;

import application.domain.models.InventoryItem;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.User;
import application.domain.valuesObjects.Money;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.WarehouseId;
import java.util.List;
import java.util.Optional;

/**
 * Entry contract of the supervisor role.
 *
 * <p>The supervisor is a read only monitoring profile: it never executes modification processes
 * (RD-ROL-06). Administrative reporting owns no identity of its own and is resolved as read only
 * queries over the existing entities.</p>
 */
public interface SupervisorPort {

    List<Order> consultOrdersByStatus(User<?> supervisor, OrderStatus status);

    List<Order> consultCompletedOrders(User<?> supervisor);

    List<Invoice> consultIssuedInvoices(User<?> supervisor);

    /**
     * @return the invoiced total, empty when no invoice has been issued yet.
     */
    Optional<Money> consultInvoicedTotal(User<?> supervisor);

    List<InventoryItem> consultStockByWarehouse(User<?> supervisor, WarehouseId warehouseId);

    List<InventoryItem> consultDamagedStock(User<?> supervisor);

    List<InventoryMovement> consultInventoryMovements(User<?> supervisor);

    List<Refund> consultRefunds(User<?> supervisor);
}
