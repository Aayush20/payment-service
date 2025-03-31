package org.example.paymentservice.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    // Payment amount in smallest currency unit.
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount should be at least 1")
    private Long amount;

    // Currency (e.g., "usd" or "inr").
    @NotBlank(message = "Currency is required")
    private String currency;

    // Payment method token or details (such as Stripe's payment method ID)
    @NotBlank(message = "Payment method is required")
    private String paymentMethodId;

    // Associated Order ID for which the payment is made.
    @NotBlank(message = "Order ID is required")
    private String orderId;

    // Desired payment gateway (for example, "stripe" or "razorpay")
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
