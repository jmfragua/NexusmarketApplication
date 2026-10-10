package application.adapters.rest.mappers;

import application.adapters.rest.dtos.responses.InvoiceResponse;
import application.adapters.rest.dtos.responses.ProductReturnResponse;
import application.adapters.rest.dtos.responses.RefundResponse;
import application.adapters.rest.dtos.responses.ShipmentResponse;
import application.domain.models.Invoice;
import application.domain.models.ProductReturn;
import application.domain.models.Refund;
import application.domain.models.Shipment;

/**
 * Translates the commercial closing and after sales entities into their REST shapes: invoice,
 * shipment, return and refund.
 */
public final class AfterSalesRestMapper {

    private AfterSalesRestMapper() {
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getIdentifier().getValue(),
                invoice.getOrderId().getValue(),
                invoice.getBuyerId().getValue(),
                invoice.getTotalAmount().getAmount(),
                invoice.getTotalAmount().getCurrency());
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getIdentifier().getValue(),
                shipment.getOrderId().getValue(),
                shipment.getWarehouseId().getValue(),
                shipment.getDeliveryAddress().getValue());
    }

    public static ProductReturnResponse toResponse(ProductReturn productReturn) {
        return new ProductReturnResponse(
                productReturn.getIdentifier().getValue(),
                productReturn.getOrderId().getValue(),
                productReturn.getOrderLineId().getValue(),
                productReturn.getQuantity().getValue());
    }

    public static RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.getIdentifier().getValue(),
                refund.getProductReturnId().getValue(),
                refund.getInvoiceId().getValue(),
                refund.getAmount().getAmount(),
                refund.getAmount().getCurrency());
    }
}
