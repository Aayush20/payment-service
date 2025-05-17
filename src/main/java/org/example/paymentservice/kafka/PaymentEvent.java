package org.example.paymentservice.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String status; // "succeeded" or "failed"
    private String paymentProvider;
    private String externalPaymentId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }
    public PaymentEvent(String orderId, String status, String paymentProvider, String externalPaymentId) {
        this.orderId = orderId;
        this.status = status;
        this.paymentProvider = paymentProvider;
        this.externalPaymentId = externalPaymentId;
    }
}
