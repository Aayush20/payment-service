package org.example.paymentservice.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.WebhookEvent;
import org.example.paymentservice.models.WebhookRetryTask;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.repositories.WebhookEventRepository;
import org.example.paymentservice.repositories.WebhookRetryTaskRepository;
import org.example.paymentservice.services.PaymentStatusService;
import org.example.paymentservice.utils.RazorpayWebhookUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/payment/webhook")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${razorpay.webhook.secret}")
    private String razorpayWebhookSecret;

    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private WebhookRetryTaskRepository webhookRetryTaskRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentStatusService paymentStatusService;

    @PostMapping("/stripe")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature: " + e.getMessage());
        }

        String eventId = event.getId();
        if (eventId == null || eventId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing event id");
        }

        if (webhookEventRepository.existsByEventId(eventId)) {
            return ResponseEntity.ok("Stripe event already processed");
        }

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                paymentStatusService.handleStripeCheckoutSessionCompleted(event);
            }
            WebhookEvent processedEvent = new WebhookEvent(eventId, LocalDateTime.now());
            webhookEventRepository.save(processedEvent);
            return ResponseEntity.ok("Stripe webhook processed successfully");
        } catch (Exception ex) {
            logger.error("Stripe webhook processing failed: {}", ex.getMessage());
            WebhookRetryTask retryTask = new WebhookRetryTask();
            retryTask.setProvider("stripe");
            retryTask.setPayload(payload);
            retryTask.setSignature(sigHeader);
            retryTask.setAttemptCount(0);
            retryTask.setProcessed(false);
            retryTask.setCreatedAt(LocalDateTime.now());
            retryTask.setUpdatedAt(LocalDateTime.now());
            webhookRetryTaskRepository.save(retryTask);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Stripe webhook enqueued for retry");
        }
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(@RequestBody String payload,
                                                  @RequestHeader("X-Razorpay-Signature") String sigHeader) {
        logger.info("Received Razorpay webhook payload: {}", payload);

        boolean isValid = RazorpayWebhookUtils.verifyWebhookSignature(payload, sigHeader, razorpayWebhookSecret);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Razorpay webhook signature");
        }

        try {
            JSONObject jsonPayload = new JSONObject(payload);
            String eventId = jsonPayload.optString("id");

            if (webhookEventRepository.existsByEventId(eventId)) {
                return ResponseEntity.ok("Razorpay event already processed");
            }

            paymentStatusService.handleRazorpayEvent(jsonPayload);

            WebhookEvent processedEvent = new WebhookEvent(eventId, LocalDateTime.now());
            webhookEventRepository.save(processedEvent);
            return ResponseEntity.ok("Razorpay webhook processed successfully");
        } catch (Exception ex) {
            logger.error("Razorpay webhook processing failed: {}", ex.getMessage());
            WebhookRetryTask retryTask = new WebhookRetryTask();
            retryTask.setProvider("razorpay");
            retryTask.setPayload(payload);
            retryTask.setSignature(sigHeader);
            retryTask.setAttemptCount(0);
            retryTask.setProcessed(false);
            retryTask.setCreatedAt(LocalDateTime.now());
            retryTask.setUpdatedAt(LocalDateTime.now());
            webhookRetryTaskRepository.save(retryTask);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Razorpay webhook enqueued for retry");
        }
    }
}
