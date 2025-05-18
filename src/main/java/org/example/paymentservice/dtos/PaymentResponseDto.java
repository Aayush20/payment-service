package org.example.paymentservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment response returned to clients")
public class PaymentResponseDto {

    @Schema(description = "Order ID", example = "order_abc123")
    private String orderId;

    @Schema(description = "External payment ID", example = "pay_abc123")
    private String paymentId;

    @Schema(description = "Payment status", example = "succeeded")
    private String status;

    @Schema(description = "Optional status message", example = "Payment completed successfully")
    private String message;

    @Schema(description = "Payment amount", example = "5000")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    private String currency;

    @Schema(description = "Payment provider", example = "stripe")
    private String provider;

    @Schema(description = "Creation timestamp", example = "2025-05-16T12:30:00")
    private LocalDateTime createdAt;

    public PaymentResponseDto() {}

    public PaymentResponseDto(String orderId, String paymentId, String status, String message, BigDecimal amount, String currency, String provider, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ✅ Utility Mapper if needed elsewhere
    public static PaymentResponseDto fromEntity(org.example.paymentservice.models.Payment p) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(p.getOrderId());
        dto.setPaymentId(p.getExternalPaymentId());
        dto.setStatus(p.getStatus().name());
        dto.setMessage("Fetched payment details successfully");
        dto.setAmount(p.getAmount() != null ? BigDecimal.valueOf(p.getAmount()) : null);
        dto.setCurrency(p.getCurrency());
        dto.setProvider(p.getPaymentProvider());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
