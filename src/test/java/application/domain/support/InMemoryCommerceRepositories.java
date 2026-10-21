package application.domain.support;

import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Shipment;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.CartId;
import application.domain.valuesObjects.CartItemId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.OrderLineId;
import application.domain.valuesObjects.OrderStatus;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.WarehouseId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory doubles of the commercial and after sales ports: cart, order, invoice, shipment, return
 * and refund.
 */
public final class InMemoryCommerceRepositories {

    private final Map<String, Cart> carts = new LinkedHashMap<>();

    private final Map<String, Order> orders = new LinkedHashMap<>();

    private final Map<String, Invoice> invoices = new LinkedHashMap<>();

    private final Map<String, Shipment> shipments = new LinkedHashMap<>();

    private final Map<String, ProductReturn> returns = new LinkedHashMap<>();

    private final Map<String, Refund> refunds = new LinkedHashMap<>();

    public CartRepositoryPort carts() {
        return new CartRepositoryPort() {

            @Override
            public Cart save(Cart cart) {
                carts.put(cart.getIdentifier().getValue(), cart);
                return cart;
            }

            @Override
            public Cart create(Cart cart) {
                if (carts.containsKey(cart.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + cart.getIdentifier());
                }
                return save(cart);
            }

            @Override
            public boolean existsCartItem(CartItemId cartItemId) {
                return carts.values().stream()
                        .flatMap(cart -> cart.getItems().stream())
                        .anyMatch(item -> item.getIdentifier().equals(cartItemId));
            }

            @Override
            public Optional<Cart> findById(CartId cartId) {
                return Optional.ofNullable(carts.get(cartId.getValue()));
            }

            @Override
            public Optional<Cart> findByBuyer(BuyerId buyerId) {
                return carts.values().stream()
                        .filter(cart -> cart.getBuyerId().equals(buyerId))
                        .findFirst();
            }
        };
    }

    public OrderRepositoryPort orders() {
        return new OrderRepositoryPort() {

            @Override
            public Order save(Order order) {
                orders.put(order.getIdentifier().getValue(), order);
                return order;
            }

            @Override
            public Order create(Order order) {
                if (orders.containsKey(order.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + order.getIdentifier());
                }
                return save(order);
            }

            @Override
            public boolean existsOrderLine(OrderLineId orderLineId) {
                return orders.values().stream()
                        .flatMap(order -> order.getLines().stream())
                        .anyMatch(line -> line.getIdentifier().equals(orderLineId));
            }

            @Override
            public Optional<Order> findById(OrderId orderId) {
                return Optional.ofNullable(orders.get(orderId.getValue()));
            }

            @Override
            public List<Order> findByBuyer(BuyerId buyerId) {
                return orders.values().stream()
                        .filter(order -> order.getBuyerId().equals(buyerId))
                        .toList();
            }

            @Override
            public List<Order> findByStatus(OrderStatus status) {
                return orders.values().stream()
                        .filter(order -> order.getStatus() == status)
                        .toList();
            }

            @Override
            public List<Order> findAll() {
                return new ArrayList<>(orders.values());
            }
        };
    }

    public InvoiceRepositoryPort invoices() {
        return new InvoiceRepositoryPort() {

            @Override
            public Invoice save(Invoice invoice) {
                invoices.put(invoice.getIdentifier().getValue(), invoice);
                return invoice;
            }

            @Override
            public Invoice create(Invoice invoice) {
                if (invoices.containsKey(invoice.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + invoice.getIdentifier());
                }
                return save(invoice);
            }

            @Override
            public Optional<Invoice> findById(InvoiceId invoiceId) {
                return Optional.ofNullable(invoices.get(invoiceId.getValue()));
            }

            @Override
            public Optional<Invoice> findByOrder(OrderId orderId) {
                return invoices.values().stream()
                        .filter(invoice -> invoice.getOrderId().equals(orderId))
                        .findFirst();
            }

            @Override
            public List<Invoice> findByBuyer(BuyerId buyerId) {
                return invoices.values().stream()
                        .filter(invoice -> invoice.getBuyerId().equals(buyerId))
                        .toList();
            }

            @Override
            public List<Invoice> findAll() {
                return new ArrayList<>(invoices.values());
            }
        };
    }

    public ShipmentRepositoryPort shipments() {
        return new ShipmentRepositoryPort() {

            @Override
            public Shipment save(Shipment shipment) {
                shipments.put(shipment.getIdentifier().getValue(), shipment);
                return shipment;
            }

            @Override
            public Shipment create(Shipment shipment) {
                if (shipments.containsKey(shipment.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + shipment.getIdentifier());
                }
                return save(shipment);
            }

            @Override
            public Optional<Shipment> findById(ShipmentId shipmentId) {
                return Optional.ofNullable(shipments.get(shipmentId.getValue()));
            }

            @Override
            public List<Shipment> findByOrder(OrderId orderId) {
                return shipments.values().stream()
                        .filter(shipment -> shipment.getOrderId().equals(orderId))
                        .toList();
            }

            @Override
            public List<Shipment> findByWarehouse(WarehouseId warehouseId) {
                return shipments.values().stream()
                        .filter(shipment -> shipment.getWarehouseId().equals(warehouseId))
                        .toList();
            }

            @Override
            public List<Shipment> findAll() {
                return new ArrayList<>(shipments.values());
            }
        };
    }

    public ProductReturnRepositoryPort returns() {
        return new ProductReturnRepositoryPort() {

            @Override
            public ProductReturn save(ProductReturn productReturn) {
                returns.put(productReturn.getIdentifier().getValue(), productReturn);
                return productReturn;
            }

            @Override
            public ProductReturn create(ProductReturn productReturn) {
                if (returns.containsKey(productReturn.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + productReturn.getIdentifier());
                }
                return save(productReturn);
            }

            @Override
            public Optional<ProductReturn> findById(ProductReturnId productReturnId) {
                return Optional.ofNullable(returns.get(productReturnId.getValue()));
            }

            @Override
            public List<ProductReturn> findByOrder(OrderId orderId) {
                return returns.values().stream()
                        .filter(productReturn -> productReturn.getOrderId().equals(orderId))
                        .toList();
            }

            @Override
            public List<ProductReturn> findAll() {
                return new ArrayList<>(returns.values());
            }
        };
    }

    public RefundRepositoryPort refunds() {
        return new RefundRepositoryPort() {

            @Override
            public Refund save(Refund refund) {
                refunds.put(refund.getIdentifier().getValue(), refund);
                return refund;
            }

            @Override
            public Refund create(Refund refund) {
                if (refunds.containsKey(refund.getIdentifier().getValue())) {
                    throw new IllegalStateException("identifier already taken: " + refund.getIdentifier());
                }
                return save(refund);
            }

            @Override
            public Optional<Refund> findById(RefundId refundId) {
                return Optional.ofNullable(refunds.get(refundId.getValue()));
            }

            @Override
            public Optional<Refund> findByProductReturn(ProductReturnId productReturnId) {
                return refunds.values().stream()
                        .filter(refund -> refund.getProductReturnId().equals(productReturnId))
                        .findFirst();
            }

            @Override
            public List<Refund> findAll() {
                return new ArrayList<>(refunds.values());
            }
        };
    }
}
