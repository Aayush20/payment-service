package org.example.paymentservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @Schema(description = "Payment amount in smallest currency unit", example = "5000")
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount should be at least 1")
    private Long amount;

    @Schema(description = "Currency code (e.g., INR, USD)", example = "INR")
    @NotBlank(message = "Currency is required")
    private String currency;

    @Schema(description = "Payment method ID (e.g., Stripe PM token)", example = "pm_1234567890")
    @NotBlank(message = "Payment method is required")
    private String paymentMethodId;

    @Schema(description = "Order ID for which the payment is made", example = "order_abc123")
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @Schema(description = "Payment provider name", example = "stripe")
    @NotBlank(message = "Payment gateway is required")
    private String gateway;

    public @NotNull(message = "Amount is required") @Min(value = 1, message = "Amount should be at least 1") Long getAmount() {
        return amount;
    }

    public void setAmount(@NotNull(message = "Amount is required") @Min(value = 1, message = "Amount should be at least 1") Long amount) {
        this.amount = amount;
    }

    public @NotBlank(message = "Currency is required") String getCurrency() {
        return currency;
    }

    public void setCurrency(@NotBlank(message = "Currency is required") String currency) {
        this.currency = currency;
    }

    public @NotBlank(message = "Payment method is required") String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(@NotBlank(message = "Payment method is required") String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public @NotBlank(message = "Order ID is required") String getOrderId() {
        return orderId;
    }

    public void setOrderId(@NotBlank(message = "Order ID is required") String orderId) {
        this.orderId = orderId;
    }

    public @NotBlank(message = "Payment gateway is required") String getGateway() {
        return gateway;
    }

    public void setGateway(@NotBlank(message = "Payment gateway is required") String gateway) {
        this.gateway = gateway;
    }
}
