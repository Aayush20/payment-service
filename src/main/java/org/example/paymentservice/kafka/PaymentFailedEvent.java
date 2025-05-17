package org.example.paymentservice.kafka;

import java.time.Instant;

public class PaymentFailedEvent {
    private String orderId;
    private String userId;
    private String provider;
    private String failureReason;
    private Instant timestamp;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(String orderId, String userId, String provider, String failureReason, Instant timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.provider = provider;
        this.failureReason = failureReason;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

}

