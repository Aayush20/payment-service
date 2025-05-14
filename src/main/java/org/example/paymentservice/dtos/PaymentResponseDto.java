package org.example.paymentservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentResponseDto {
    @Schema(description = "Internal payment ID", example = "pay_abc123")
    private String paymentId;

    @Schema(description = "Status of the payment", example = "succeeded")
    private String status;

    @Schema(description = "Status message or description", example = "Payment completed successfully")
    private String message;

    public PaymentResponseDto(String paymentId, String status, String message) {
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
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
}

