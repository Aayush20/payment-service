package org.example.paymentservice.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "webhook_retry_tasks")
public class WebhookRetryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider; // "stripe" or "razorpay"
    private String payload; // full payload
    private String signature; // X-Razorpay-Signature or Stripe-Signature
    private int attemptCount;
    private boolean processed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static WebhookRetryTask buildStripeRetry(String payload, String sigHeader) {
        WebhookRetryTask task = new WebhookRetryTask();
        task.setProvider("stripe");
        task.setPayload(payload);
        task.setSignature(sigHeader);
        task.setAttemptCount(0);
        task.setProcessed(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    public static WebhookRetryTask buildRazorpayRetry(String payload, String sigHeader) {
        WebhookRetryTask task = new WebhookRetryTask();
        task.setProvider("razorpay");
        task.setPayload(payload);
        task.setSignature(sigHeader);
        task.setAttemptCount(0);
        task.setProcessed(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
