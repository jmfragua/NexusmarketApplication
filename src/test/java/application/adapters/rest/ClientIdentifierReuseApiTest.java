package application.adapters.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.InventoryItemRepositoryPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ProductReturnRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.models.SellerWarehouse;
import application.domain.valuesObjects.BuyerId;
import application.domain.valuesObjects.InventoryItemId;
import application.domain.valuesObjects.InventoryMovementId;
import application.domain.valuesObjects.InvoiceId;
import application.domain.valuesObjects.OrderId;
import application.domain.valuesObjects.ProductId;
import application.domain.valuesObjects.ProductReturnId;
import application.domain.valuesObjects.RefundId;
import application.domain.valuesObjects.SellerId;
import application.domain.valuesObjects.ShipmentId;
import application.domain.valuesObjects.UserId;
import application.domain.valuesObjects.WarehouseId;
import application.support.ApiTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Identifiers of new entities are chosen by the client (RD-POUT-04). A creation that reuses one must
 * answer 409 and leave the existing entity exactly as it was: a creation never acts as an update.
 *
 * <p>Seller A owns product {@code p-a} (variant {@code v-a}) and stock record {@code inv-a}; buyer X
 * owns cart item {@code ci-x} and order {@code o-x} (line {@code ol-x}). Seller B and buyer Y try to
 * reuse those identifiers.</p>
 */
class ClientIdentifierReuseApiTest extends ApiTestSupport {

    @Autowired private ProductRepositoryPort products;
    @Autowired private CartRepositoryPort carts;
    @Autowired private OrderRepositoryPort orders;
    @Autowired private WarehouseRepositoryPort warehouses;
    @Autowired private InvoiceRepositoryPort invoices;
    @Autowired private ShipmentRepositoryPort shipments;
    @Autowired private ProductReturnRepositoryPort returns;
    @Autowired private RefundRepositoryPort refunds;
    @Autowired private InventoryItemRepositoryPort inventoryItems;
    @Autowired private InventoryMovementRepositoryPort movements;

    private String admin;
    private String sellerA;
    private String sellerB;
    private String buyerX;
    private String buyerY;
    private String operator;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
        sellerA = sellerToken(admin, "a");
        sellerB = sellerToken(admin, "b");
        buyerX = buyerToken("x");
        buyerY = buyerToken("y");
        mockMvc.perform(authorized(post("/api/v1/admin/staff"), admin).content("""
                        {"userId":"ops","fullName":"Operador","email":"ops@staff.test","identityDocument":"DOC-OPS",
                         "password":"staff-secret","role":"LOGISTICS_OPERATOR"}"""))
                .andExpect(status().isCreated());
        operator = login("ops@staff.test", "staff-secret");

        product(sellerA, "p-a", "v-a", "Morral A");
        product(sellerB, "p-b", "v-b", "Termo B");
        expect201(post("/api/v1/seller/stock/items"), sellerA, "{\"inventoryItemId\":\"inv-a\",\"productId\":\"p-a\",\"warehouseId\":\"wh-a\"}");
        expect201(post("/api/v1/seller/stock/receipts"), sellerA, "{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-a\",\"quantity\":10}");
        expect201(post("/api/v1/buyer/me/cart/items"), buyerX, cartItem("ci-x", "p-a", "v-a", 2));
    }

    private void product(String token, String productId, String variantId, String name) throws Exception {
        expect201(post("/api/v1/seller/products"), token, "{\"productId\":\"%s\",\"productType\":\"PHYSICAL\"}".formatted(productId));
        expect201(post("/api/v1/seller/products/" + productId + "/variants"), token,
                "{\"variantId\":\"%s\",\"attributes\":{\"name\":\"%s\",\"price\":\"1000\"}}".formatted(variantId, name));
    }

    private static String cartItem(String id, String productId, String variantId, int quantity) {
        return "{\"cartItemId\":\"%s\",\"productId\":\"%s\",\"productVariantId\":\"%s\",\"quantity\":%d}"
                .formatted(id, productId, variantId, quantity);
    }

    private static String order(String orderId, String lineId, String productId, String variantId, int quantity) {
        return """
                {"orderId":"%s","deliveryAddress":"Calle 1 #2-3","lines":[{"orderLineId":"%s","productId":"%s",
                 "productVariantId":"%s","quantity":%d,"lineAmount":%d,"currency":"COP"}]}"""
                .formatted(orderId, lineId, productId, variantId, quantity, quantity * 1000);
    }

    private void expect201(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                           String token, String body) throws Exception {
        mockMvc.perform(authorized(request, token).content(body)).andExpect(status().isCreated());
    }

    private void expect409(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                           String token, String body) throws Exception {
        mockMvc.perform(authorized(request, token).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.rule").isNotEmpty());
    }

    /** X orders and pays o-x; with {@code deliver}, logistics packs (shp-x), dispatches and delivers it. */
    private void paidOrderOfX(boolean deliver) throws Exception {
        expect201(post("/api/v1/buyer/me/orders"), buyerX, order("o-x", "ol-x", "p-a", "v-a", 2));
        mockMvc.perform(authorized(post("/api/v1/buyer/me/orders/o-x/payment"), buyerX)).andExpect(status().isOk());
        if (deliver) {
            deliver("o-x", "shp-x");
        }
    }

    private void paidOrderOfY(boolean deliver) throws Exception {
        expect201(post("/api/v1/buyer/me/cart/items"), buyerY, cartItem("ci-y", "p-b", "v-b", 1));
        expect201(post("/api/v1/buyer/me/orders"), buyerY, order("o-y", "ol-y", "p-b", "v-b", 1));
        mockMvc.perform(authorized(post("/api/v1/buyer/me/orders/o-y/payment"), buyerY)).andExpect(status().isOk());
        if (deliver) {
            deliver("o-y", "shp-y");
        }
    }

    private void deliver(String orderId, String shipmentId) throws Exception {
        String body = "{\"shipmentId\":\"%s\",\"orderId\":\"%s\",\"warehouseId\":\"wh-a\",\"deliveryAddress\":\"Calle 1 #2-3\"}"
                .formatted(shipmentId, orderId);
        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/packaging"), operator).content(body)).andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/dispatches"), operator).content(body)).andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/deliveries"), operator).content(body)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("product: seller B cannot register a product with the id of seller A's product")
    void product() throws Exception {
        expect409(post("/api/v1/seller/products"), sellerB, "{\"productId\":\"p-a\",\"productType\":\"DIGITAL\"}");

        Product original = products.findById(ProductId.of("p-a")).orElseThrow();
        assertEquals(SellerId.of("s-a"), original.getSellerId());
        assertEquals(1, original.getVariants().size());
        assertTrue(original.requiresInventory(), "it is still the physical product of seller A");
    }

    @Test
    @DisplayName("variant: seller B cannot add a variant with the id of seller A's variant")
    void variant() throws Exception {
        expect409(post("/api/v1/seller/products/p-b/variants"), sellerB,
                "{\"variantId\":\"v-a\",\"attributes\":{\"name\":\"Robado\",\"price\":\"1\"}}");

        Product original = products.findById(ProductId.of("p-a")).orElseThrow();
        assertEquals(1, original.getVariants().size());
        assertEquals("v-a", original.getVariants().get(0).getIdentifier().getValue());
        assertEquals("Morral A", original.getVariants().get(0).getAttributes().stream()
                .filter(a -> a.getName().equals("name")).findFirst().orElseThrow().getValue());
        assertEquals(1, products.findById(ProductId.of("p-b")).orElseThrow().getVariants().size());
    }

    @Test
    @DisplayName("cart item: buyer Y cannot add a cart item with the id of buyer X's item")
    void cartItem() throws Exception {
        expect409(post("/api/v1/buyer/me/cart/items"), buyerY, cartItem("ci-x", "p-b", "v-b", 7));

        var cartOfX = carts.findByBuyer(BuyerId.of("b-x")).orElseThrow();
        assertEquals(1, cartOfX.getItems().size());
        assertEquals(2, cartOfX.getItems().get(0).getQuantity().getValue());
        assertTrue(carts.findByBuyer(BuyerId.of("b-y")).orElseThrow().getItems().isEmpty());
    }

    @Test
    @DisplayName("order: buyer Y cannot place an order with the id of buyer X's order")
    void order() throws Exception {
        paidOrderOfX(false);
        expect201(post("/api/v1/buyer/me/cart/items"), buyerY, cartItem("ci-y", "p-b", "v-b", 1));

        expect409(post("/api/v1/buyer/me/orders"), buyerY, order("o-x", "ol-new", "p-b", "v-b", 1));

        Order original = orders.findById(OrderId.of("o-x")).orElseThrow();
        assertEquals(BuyerId.of("b-x"), original.getBuyerId());
        assertEquals("PAID", original.getStatus().code());
        assertEquals("ol-x", original.getLines().get(0).getIdentifier().getValue());
        assertEquals(1, carts.findByBuyer(BuyerId.of("b-y")).orElseThrow().getItems().size(),
                "a rejected confirmation keeps the cart of Y");
    }

    @Test
    @DisplayName("order line: buyer Y cannot reuse the id of a line of buyer X's order")
    void orderLine() throws Exception {
        paidOrderOfX(false);
        expect201(post("/api/v1/buyer/me/cart/items"), buyerY, cartItem("ci-y", "p-b", "v-b", 1));

        expect409(post("/api/v1/buyer/me/orders"), buyerY, order("o-y", "ol-x", "p-b", "v-b", 1));

        Order original = orders.findById(OrderId.of("o-x")).orElseThrow();
        assertEquals(1, original.getLines().size());
        assertEquals(ProductId.of("p-a"), original.getLines().get(0).getProductId());
        assertTrue(orders.findById(OrderId.of("o-y")).isEmpty());
    }

    @Test
    @DisplayName("warehouse: the administrator cannot register a warehouse with the id of a seller's warehouse")
    void marketplaceWarehouse() throws Exception {
        expect409(post("/api/v1/admin/warehouses"), admin, "{\"warehouseId\":\"wh-a\"}");

        SellerWarehouse original = (SellerWarehouse) warehouses.findById(WarehouseId.of("wh-a")).orElseThrow();
        assertEquals(SellerId.of("s-a"), original.getSellerId());
    }

    @Test
    @DisplayName("first warehouse: a seller cannot be incorporated with an existing warehouse id")
    void firstWarehouseOfSeller() throws Exception {
        expect409(post("/api/v1/admin/sellers"), admin, """
                {"sellerId":"s-c","fullName":"Tienda C","email":"c@sellers.test","identityDocument":"DOC-S-c",
                 "password":"seller-secret","firstWarehouseId":"wh-a"}""");

        SellerWarehouse original = (SellerWarehouse) warehouses.findById(WarehouseId.of("wh-a")).orElseThrow();
        assertEquals(SellerId.of("s-a"), original.getSellerId());
        assertTrue(userRepositoryPort.findByIdentifier(UserId.of("s-c")).isEmpty(), "no seller without its warehouse");
    }

    @Test
    @DisplayName("invoice: the administrator cannot issue an invoice with the id of an existing invoice")
    void invoice() throws Exception {
        paidOrderOfX(false);
        paidOrderOfY(false);
        expect201(post("/api/v1/admin/invoices"), admin, "{\"invoiceId\":\"inv-1\",\"orderId\":\"o-x\"}");

        expect409(post("/api/v1/admin/invoices"), admin, "{\"invoiceId\":\"inv-1\",\"orderId\":\"o-y\"}");

        var original = invoices.findById(InvoiceId.of("inv-1")).orElseThrow();
        assertEquals(OrderId.of("o-x"), original.getOrderId());
        assertEquals(0, new BigDecimal("2000").compareTo(original.getTotalAmount().getAmount()));
    }

    @Test
    @DisplayName("shipment: logistics cannot pack an order into the shipment of another order")
    void shipment() throws Exception {
        paidOrderOfX(false);
        paidOrderOfY(false);
        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/packaging"), operator).content(
                        "{\"shipmentId\":\"shp-1\",\"orderId\":\"o-x\",\"warehouseId\":\"wh-a\",\"deliveryAddress\":\"Calle 1 #2-3\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(post("/api/v1/logistics/shipments/packaging"), operator).content(
                        "{\"shipmentId\":\"shp-1\",\"orderId\":\"o-y\",\"warehouseId\":\"wh-b\",\"deliveryAddress\":\"Calle 1 #2-3\"}"))
                .andExpect(status().isConflict());

        var original = shipments.findById(ShipmentId.of("shp-1")).orElseThrow();
        assertEquals(OrderId.of("o-x"), original.getOrderId());
        assertEquals(WarehouseId.of("wh-a"), original.getWarehouseId());
    }

    @Test
    @DisplayName("return and refund: their ids cannot be reused for another order")
    void returnAndRefund() throws Exception {
        paidOrderOfX(true);
        paidOrderOfY(true);
        expect201(post("/api/v1/buyer/me/returns"), buyerX, "{\"returnId\":\"r-1\",\"orderId\":\"o-x\",\"orderLineId\":\"ol-x\",\"quantity\":1}");

        expect409(post("/api/v1/buyer/me/returns"), buyerY, "{\"returnId\":\"r-1\",\"orderId\":\"o-y\",\"orderLineId\":\"ol-y\",\"quantity\":1}");
        var originalReturn = returns.findById(ProductReturnId.of("r-1")).orElseThrow();
        assertEquals(OrderId.of("o-x"), originalReturn.getOrderId());

        expect201(post("/api/v1/buyer/me/returns"), buyerY, "{\"returnId\":\"r-2\",\"orderId\":\"o-y\",\"orderLineId\":\"ol-y\",\"quantity\":1}");
        expect201(post("/api/v1/admin/invoices"), admin, "{\"invoiceId\":\"inv-x\",\"orderId\":\"o-x\"}");
        expect201(post("/api/v1/admin/invoices"), admin, "{\"invoiceId\":\"inv-y\",\"orderId\":\"o-y\"}");
        expect201(post("/api/v1/admin/refunds"), admin, "{\"refundId\":\"rf-1\",\"productReturnId\":\"r-1\",\"invoiceId\":\"inv-x\",\"amount\":500,\"currency\":\"COP\"}");

        expect409(post("/api/v1/admin/refunds"), admin, "{\"refundId\":\"rf-1\",\"productReturnId\":\"r-2\",\"invoiceId\":\"inv-y\",\"amount\":900,\"currency\":\"COP\"}");
        var originalRefund = refunds.findById(RefundId.of("rf-1")).orElseThrow();
        assertEquals(ProductReturnId.of("r-1"), originalRefund.getProductReturnId());
        assertEquals(0, new BigDecimal("500").compareTo(originalRefund.getAmount().getAmount()));
    }

    @Test
    @DisplayName("movement: a stock operation cannot reuse the id of an existing movement")
    void movement() throws Exception {
        expect201(post("/api/v1/seller/stock/items"), sellerB, "{\"inventoryItemId\":\"inv-b\",\"productId\":\"p-b\",\"warehouseId\":\"wh-b\"}");

        expect409(post("/api/v1/seller/stock/receipts"), sellerB, "{\"inventoryItemId\":\"inv-b\",\"movementId\":\"mov-a\",\"quantity\":4}");
        expect409(post("/api/v1/seller/stock/receipts"), sellerA, "{\"inventoryItemId\":\"inv-a\",\"movementId\":\"mov-a\",\"quantity\":4}");

        var original = movements.findById(InventoryMovementId.of("mov-a")).orElseThrow();
        assertEquals(InventoryItemId.of("inv-a"), original.getInventoryItemId());
        assertEquals(10, original.getQuantity().getValue());
        assertEquals(10, inventoryItems.findById(InventoryItemId.of("inv-a")).orElseThrow().getAvailableQuantity().getValue());
        assertEquals(0, inventoryItems.findById(InventoryItemId.of("inv-b")).orElseThrow().getAvailableQuantity().getValue());
        mockMvc.perform(authorized(get("/api/v1/seller/products/p-b/stock"), sellerB))
                .andExpect(jsonPath("$.content[0].availableQuantity").value(0));
    }
}
