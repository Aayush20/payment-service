package org.example.paymentservice.controllers;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.paymentservice.models.WebhookEvent;
import org.example.paymentservice.models.WebhookRetryTask;
import org.example.paymentservice.repositories.*;
import org.example.paymentservice.services.*;
import org.example.paymentservice.utils.RazorpayWebhookUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Webhook API", description = "Handles incoming Stripe and Razorpay webhooks")
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
    @Autowired private PaymentStatusService paymentStatusService;
    @Autowired private RateLimiterService rateLimiterService;

    @Operation(
            summary = "Stripe Webhook",
            description = "Handles incoming Stripe webhook and updates payment status",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "Stripe Event", value = "{ \"id\": \"evt_1\", \"type\": \"checkout.session.completed\" }")
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid signature or missing data"),
                    @ApiResponse(responseCode = "409", description = "Duplicate event"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                    @ApiResponse(responseCode = "500", description = "Internal error")
            }
    )
    @PostMapping("/stripe")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        Bucket bucket = rateLimiterService.resolveBucket("stripe-webhook");
        if (!bucket.tryConsume(1)) {
            logger.warn("Stripe webhook rate limit exceeded");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature: " + e.getMessage());
        }

        String eventId = event.getId();
        if (eventId == null || eventId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing event id");
        }

        if (webhookEventRepository.findByEventId(eventId).isPresent()) {
            logger.warn("Duplicate Stripe webhook event received. Skipping. eventId={}", eventId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate Stripe event");
        }

        try {
            if ("checkout.session.completed".equals(event.getType())) {
                paymentStatusService.handleStripeCheckoutSessionCompleted(event);
            }
            webhookEventRepository.save(new WebhookEvent(eventId, LocalDateTime.now()));
            return ResponseEntity.ok("Stripe webhook processed successfully");
        } catch (Exception ex) {
            logger.error("Stripe webhook processing failed: {}", ex.getMessage());
            webhookRetryTaskRepository.save(WebhookRetryTask.buildStripeRetry(payload, sigHeader));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Stripe webhook enqueued for retry");
        }
    }

    @Operation(
            summary = "Razorpay Webhook",
            description = "Handles Razorpay webhook for payment confirmation",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "Razorpay Event", value = "{ \"id\": \"evt_razorpay_123\" }")
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Webhook processed"),
                    @ApiResponse(responseCode = "400", description = "Invalid signature or missing data"),
                    @ApiResponse(responseCode = "409", description = "Duplicate event"),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                    @ApiResponse(responseCode = "500", description = "Processing failed, task queued")
            }
    )
    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(@RequestBody String payload,
                                                  @RequestHeader("X-Razorpay-Signature") String sigHeader) {
        Bucket bucket = rateLimiterService.resolveBucket("razorpay-webhook");
        if (!bucket.tryConsume(1)) {
            logger.warn("Razorpay webhook rate limit exceeded");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }

        logger.info("Received Razorpay webhook payload: {}", payload);
        boolean isValid = RazorpayWebhookUtils.verifyWebhookSignature(payload, sigHeader, razorpayWebhookSecret);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Razorpay webhook signature");
        }

        try {
            JSONObject jsonPayload = new JSONObject(payload);
            String eventId = jsonPayload.optString("id");

            if (webhookEventRepository.findByEventId(eventId).isPresent()) {
                logger.warn("Duplicate Razorpay webhook event received. Skipping. eventId={}", eventId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate Razorpay event");
            }

            paymentStatusService.handleRazorpayEvent(jsonPayload);
            webhookEventRepository.save(new WebhookEvent(eventId, LocalDateTime.now()));
            return ResponseEntity.ok("Razorpay webhook processed successfully");
        } catch (Exception ex) {
            logger.error("Razorpay webhook processing failed: {}", ex.getMessage());
            webhookRetryTaskRepository.save(WebhookRetryTask.buildRazorpayRetry(payload, sigHeader));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Razorpay webhook enqueued for retry");
        }
    }
}
