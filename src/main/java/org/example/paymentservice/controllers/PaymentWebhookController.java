//package org.example.paymentservice.controllers;
//
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.model.Event;
//import com.stripe.net.Webhook;
//import jakarta.servlet.http.HttpServletRequest;
//import java.time.LocalDateTime;
//
//import org.example.paymentservice.models.WebhookEvent;
//import org.example.paymentservice.repositories.WebhookEventRepository;
//import org.example.paymentservice.services.PaymentStatusService;
//import org.example.paymentservice.utils.RazorpayWebhookUtils;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/payment/webhook")
//public class PaymentWebhookController {
//
//    // Stripe webhook secret from application.properties (e.g., stripe.webhook.secret=whsec_yourSecret)
//    @Value("${stripe.webhook.secret}")
//    private String stripeWebhookSecret;
//
//    // Razorpay webhook secret from application.properties (e.g., razorpay.webhook.secret=yourRazorpayWebhookSecret)
//    @Value("${razorpay.webhook.secret}")
//    private String razorpayWebhookSecret;
//
//    private final WebhookEventRepository webhookEventRepository;
//    private final PaymentStatusService paymentStatusService;
//
//    public PaymentWebhookController(WebhookEventRepository webhookEventRepository,
//                                    PaymentStatusService paymentStatusService) {
//        this.webhookEventRepository = webhookEventRepository;
//        this.paymentStatusService = paymentStatusService;
//    }
//
//    /**
//     * Stripe webhook endpoint.
//     *
//     * IMPORTANT: The incoming payload should be verified using Stripe's signature header.
//     */
//    @PostMapping("/stripe")
//    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
//                                                @RequestHeader("Stripe-Signature") String sigHeader) {
//        Event event;
//        try {
//            // Verify the webhook signature using Stripe SDK's helper method.
//            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
//        } catch (SignatureVerificationException e) {
//            // Log the error (configuration commented out below)
//            // logger.error("Stripe signature verification failed", e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("Invalid signature: " + e.getMessage());
//        }
//
//        // Idempotency check: if this event has already been processed, skip it.
//        String eventId = event.getId();
//        if (webhookEventRepository.existsByEventId(eventId)) {
//            return ResponseEntity.ok("Stripe event already processed");
//        }
//
//        // Process the event according to its type.
//        if ("checkout.session.completed".equals(event.getType())) {
//            // Pass the event to a service that is responsible for updating the Payment record.
//            paymentStatusService.handleStripeCheckoutSessionCompleted(event);
//        }
//        // Add additional event type handling as needed.
//
//        // Record the event processing for idempotency purposes.
//        WebhookEvent processedEvent = new WebhookEvent();
//        processedEvent.setEventId(eventId);
//        processedEvent.setProcessedAt(LocalDateTime.now());
//        webhookEventRepository.save(processedEvent);
//
//        return ResponseEntity.ok("Stripe webhook processed successfully");
//    }
//
//    /**
//     * Razorpay webhook endpoint.
//     *
//     * IMPORTANT: Verify the Razorpay signature using your helper method.
//     */
//    @PostMapping("/razorpay")
//    public ResponseEntity<String> razorpayWebhook(@RequestBody String payload,
//                                                  @RequestHeader("X-Razorpay-Signature") String sigHeader) {
//        // Verify the webhook signature with the custom utility (see RazorpayWebhookUtils)
//        boolean isValid = RazorpayWebhookUtils.verifyWebhookSignature(payload, sigHeader, razorpayWebhookSecret);
//        if (!isValid) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Razorpay webhook signature");
//        }
//
//        // Parse the payload (using org.json) and extract event id for idempotency check.
//        JSONObject jsonPayload = new JSONObject(payload);
//        String eventId = jsonPayload.optString("id");
//        if (webhookEventRepository.existsByEventId(eventId)) {
//            return ResponseEntity.ok("Razorpay event already processed");
//        }
//
//        // Process the event by delegating to your PaymentStatusService.
//        paymentStatusService.handleRazorpayEvent(jsonPayload);
//
//        // Record the event to ensure idempotency.
//        WebhookEvent processedEvent = new WebhookEvent();
//        processedEvent.setEventId(eventId);
//        processedEvent.setProcessedAt(LocalDateTime.now());
//        webhookEventRepository.save(processedEvent);
//
//        return ResponseEntity.ok("Razorpay webhook processed successfully");
//    }
//}

package org.example.paymentservice.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.WebhookEvent;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.repositories.WebhookEventRepository;
import org.example.paymentservice.services.PaymentStatusService;
import org.example.paymentservice.utils.RazorpayWebhookUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/webhook")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    // Stripe webhook secret from application.properties (e.g., stripe.webhook.secret=whsec_yourSecret)
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    // Razorpay webhook secret from application.properties (e.g., razorpay.webhook.secret=yourRazorpayWebhookSecret)
    @Value("${razorpay.webhook.secret}")
    private String razorpayWebhookSecret;

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentStatusService paymentStatusService;

    @Autowired
    private  PaymentRepository paymentRepository;

    public PaymentWebhookController(WebhookEventRepository webhookEventRepository,
                                    PaymentStatusService paymentStatusService) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentStatusService = paymentStatusService;
    }

    /**
     * Stripe webhook endpoint.
     * <p>
     * IMPORTANT: The incoming payload should be verified using Stripe's signature header.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            // Verify the webhook signature using Stripe SDK's helper method.
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature: " + e.getMessage());
        }

        String eventId = event.getId();
        if (eventId == null || eventId.trim().isEmpty()) {
            logger.error("Received Stripe event with missing event id.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing event id");
        }

        // Idempotency check: if this event has already been processed, skip it.
        if (webhookEventRepository.existsByEventId(eventId)) {
            logger.info("Stripe event with id {} has already been processed.", eventId);
            return ResponseEntity.ok("Stripe event already processed");
        }

        // Process the event according to its type.
        if ("checkout.session.completed".equals(event.getType())) {
            paymentStatusService.handleStripeCheckoutSessionCompleted(event);
        } else {
            logger.warn("Unhandled Stripe event type: {}", event.getType());
        }

        // Record the event processing for idempotency purposes.
        WebhookEvent processedEvent = new WebhookEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setProcessedAt(LocalDateTime.now());
        try {
            webhookEventRepository.save(processedEvent);
        } catch (Exception e) {
            logger.error("Failed to save webhook event for Stripe event id {}. Error: {}", eventId, e.getMessage());
        }

        return ResponseEntity.ok("Stripe webhook processed successfully");
    }

    /**
     * Razorpay webhook endpoint.
     * <p>
     * IMPORTANT: Verify the Razorpay signature using your helper method.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(@RequestBody String payload,
                                                  @RequestHeader("X-Razorpay-Signature") String sigHeader) {
        logger.info("Received Razorpay webhook payload: {}", payload);

        // Verify the Razorpay signature using your utility method
        boolean isValid = RazorpayWebhookUtils.verifyWebhookSignature(payload, sigHeader, razorpayWebhookSecret);
        if (!isValid) {
            logger.error("Invalid Razorpay webhook signature.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Razorpay webhook signature");
        }

        // Parse the payload into a JSON object
        JSONObject jsonPayload = new JSONObject(payload);

        // Extract event type
        String eventType = jsonPayload.optString("event", "").trim();
        if (eventType.isEmpty()) {
            logger.error("Razorpay webhook payload missing event type.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing event type in payload.");
        }

        // Extract reference_id from the payment_link entity
        JSONObject paymentLinkEntity = jsonPayload
                .optJSONObject("payload")
                .optJSONObject("payment_link")
                .optJSONObject("entity");

        if (paymentLinkEntity == null) {
            logger.error("Razorpay webhook payload missing payment_link.entity.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing payment_link.entity in payload.");
        }

        String referenceId = paymentLinkEntity.optString("reference_id", "").trim();
        if (referenceId.isEmpty()) {
            logger.error("Razorpay webhook payload missing reference_id.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing reference_id in payment_link.entity.");
        }

        // Process the webhook based on the reference_id
        logger.info("Processing Razorpay event with reference_id: {}", referenceId);

        // Use reference_id to locate the Payment record in your database
        Payment payment = paymentRepository.findByOrderId(referenceId);
        if (payment != null) {
            payment.setStatus("succeeded");
            payment.setExternalPaymentId(paymentLinkEntity.optString("id")); // Add payment link ID for tracking
            paymentRepository.save(payment);
            logger.info("Updated Payment record for reference_id: {}", referenceId);
        } else {
            logger.warn("No Payment record found for reference_id: {}", referenceId);
        }

        // Optionally record the webhook event for idempotency
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setEventId(referenceId); // Use reference_id as a unique identifier for Razorpay events
        webhookEvent.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(webhookEvent);

        return ResponseEntity.ok("Razorpay webhook processed successfully.");
    }
}

