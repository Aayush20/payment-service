package org.example.paymentservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "payments")
public class Payment extends BaseModel {

    // Associated Order ID from Order Service
    @Column(nullable = false)
    private String orderId;

    // Payment provider (e.g., "stripe" or "razorpay")
    @Column(nullable = false)
    private String paymentProvider;

    // Payment amount in the smallest currency unit (e.g., cents or paise)
    @Column(nullable = false)
    private Long amount;

    // Currency code (e.g., "usd", "inr")
    @Column(nullable = false)
    private String currency;

    // Status: "pending", "succeeded", "failed", etc.
    @Column(nullable = false)
    private String status;

    // External payment identifier returned by Stripe or Razorpay.
    // Increase the length to accommodate a longer URL.
    @Column(length = 512)
    private String externalPaymentId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }
}
