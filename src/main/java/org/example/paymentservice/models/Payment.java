package org.example.paymentservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "payments")
@EqualsAndHashCode(callSuper = true)
public class Payment extends BaseModel {

    // Associated Order ID from Order Service
    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;


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



    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
