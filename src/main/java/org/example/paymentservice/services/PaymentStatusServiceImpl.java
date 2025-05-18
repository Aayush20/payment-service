package org.example.paymentservice.services;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.kafka.PaymentEventPublisher;
import org.example.paymentservice.kafka.PaymentEvent;
import org.example.paymentservice.kafka.PaymentFailedEvent;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentAuditLog;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentAuditLogRepository;
import org.example.paymentservice.repositories.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentStatusServiceImpl implements PaymentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusServiceImpl.class);

    private static final String USER_PAYMENT_CACHE_PREFIX = "user:payments:";

    @Value("${cache.ttl.userPayments:60}") // TTL in seconds
    private long userPaymentCacheTtl;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private PaymentAuditLogRepository paymentAuditLogRepository;

    @Autowired
    private SendGridEmailService emailService;


    @Override
    public void handleStripeCheckoutSessionCompleted(Event event) {
        logger.info("Received Stripe event: {}", event);

        Optional<StripeObject> optionalSession = event.getDataObjectDeserializer().getObject();
        if (!optionalSession.isPresent()) {
            logger.error("Failed to deserialize session from Stripe event.");
            return;
        }

        Session session;
        try {
            session = (Session) optionalSession.get();
        } catch (ClassCastException e) {
            logger.error("Deserialized object is not of type Session: {}", e.getMessage());
            return;
        }

        if (session.getMetadata() == null || !session.getMetadata().containsKey("orderId")) {
            logger.error("Missing orderId in session metadata.");
            return;
        }

        String orderId = session.getMetadata().get("orderId").trim();
        if (orderId.isEmpty()) {
            logger.error("Empty orderId received from Stripe metadata.");
            return;
        }

        String externalPaymentId = session.getId();
        Payment payment = paymentRepository.findByOrderId(orderId);

        if (payment == null) {
            logger.warn("No Payment record found for orderId: {}", orderId);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    orderId,
                    "unknown", // or extract from metadata if available
                    "stripe",
                    "No matching payment found for orderId",
                    Instant.now()
            );
            paymentEventPublisher.publishPaymentFailedEvent(failedEvent);
        }

        // Set MDC context for logging
        MDC.put("orderId", orderId);
        MDC.put("provider", "stripe");
        MDC.put("externalPaymentId", externalPaymentId);
        MDC.put("traceId", UUID.randomUUID().toString());
        MDC.put("userId", payment.getUserId());


        try {
            if (payment != null) {
                payment.setStatus(PaymentStatus.SUCCEEDED);

                payment.setExternalPaymentId(externalPaymentId);
                paymentRepository.save(payment);
                PaymentAuditLog auditLog = new PaymentAuditLog(
                        orderId,
                        payment.getUserId(),
                        payment.getPaymentProvider(),
                        payment.getAmount(),
                        PaymentStatus.SUCCEEDED.name(),
                        LocalDateTime.now(),
                        "Payment succeeded with externalPaymentId: " + externalPaymentId
                );
                paymentAuditLogRepository.save(auditLog);


                logger.info("Payment updated successfully for Stripe order.");

                // Send confirmation email after payment success
                try {
                    emailService.sendEmail(
                            payment.getUserEmail(), // ✅ Real user email stored in DB
                            "✅ Payment Successful - Order #" + payment.getOrderId(),
                            "Your payment of " + payment.getAmount() + " " + payment.getCurrency().toUpperCase() +
                                    " via Stripe was successful for Order ID: " + payment.getOrderId() +
                                    ".\n\nThank you for shopping with us!"
                    );
                } catch (IOException ex) {
                    logger.error("❌ Failed to send payment success email for order {}: {}", orderId, ex.getMessage());
                }


                paymentEventPublisher.publishPaymentSuccess(new PaymentEvent(
                        orderId,
                        "succeeded",
                        payment.getPaymentProvider(),
                        externalPaymentId
                ));
            } else {
                logger.warn("No Payment record found for orderId: {}", orderId);
                // ⚠️ Fallback email if payment not found
                try {
                    emailService.sendEmail(
                            orderId + "@fallbackmail.com", // ⛔ Replace with real fallback if available
                            "❌ Payment Failed - Unknown Order",
                            "We received a payment intent for Order ID: " + orderId + ", but no matching order was found. Please contact support if this was unexpected."
                    );
                } catch (IOException e) {
                    logger.warn("Failed to send fallback email for unknown payment: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error updating payment for orderId {}: {}", orderId, e.getMessage());

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    orderId,
                    payment != null ? payment.getUserId() : "unknown",
                    "stripe",
                    "Exception during Stripe processing: " + e.getMessage(),
                    Instant.now()
            );
            paymentEventPublisher.publishPaymentFailedEvent(failedEvent);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void handleRazorpayEvent(JSONObject eventPayload) {
        logger.info("Received Razorpay webhook payload: {}", eventPayload);

        String eventType = eventPayload.optString("event", "").trim();
        if (!"payment_link.paid".equals(eventType)) {
            logger.warn("Unhandled Razorpay event type: {}", eventType);
            return;
        }

        JSONObject paymentLinkObj = eventPayload
                .optJSONObject("payload")
                .optJSONObject("payment_link");

        if (paymentLinkObj == null) {
            logger.error("Missing payment_link in Razorpay payload.");
            return;
        }

        String orderId = paymentLinkObj.optString("reference_id", "").trim();
        String externalPaymentId = paymentLinkObj.optString("id", "").trim();

        if (orderId.isEmpty() || externalPaymentId.isEmpty()) {
            logger.error("Missing reference_id or payment_link.id in Razorpay payload.");
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId);

        if (payment == null) {
            logger.warn("No Payment record found for Razorpay orderId: {}", orderId);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    orderId,
                    "unknown",
                    "razorpay",
                    "No matching payment found for Razorpay orderId",
                    Instant.now()
            );
            paymentEventPublisher.publishPaymentFailedEvent(failedEvent);
        }


        // Set MDC context for logging
        MDC.put("orderId", orderId);
        MDC.put("provider", "razorpay");
        MDC.put("externalPaymentId", externalPaymentId);
        MDC.put("traceId", UUID.randomUUID().toString());
        MDC.put("userId", payment.getUserId());

        try {
            if (payment != null) {
                payment.setStatus(PaymentStatus.SUCCEEDED);

                payment.setExternalPaymentId(externalPaymentId);
                paymentRepository.save(payment);

                PaymentAuditLog auditLog = new PaymentAuditLog(
                        orderId,
                        payment.getUserId(),
                        payment.getPaymentProvider(),
                        payment.getAmount(),
                        PaymentStatus.SUCCEEDED.name(),
                        LocalDateTime.now(),
                        "Payment succeeded with externalPaymentId: " + externalPaymentId
                );
                paymentAuditLogRepository.save(auditLog);



                logger.info("Payment updated successfully for Razorpay order.");

                // Send confirmation email after Razorpay success
                try {
                    emailService.sendEmail(
                            payment.getUserEmail(), // ✅ Real user email stored in DB
                            "✅ Payment Successful - Order #" + payment.getOrderId(),
                            "Your payment of " + payment.getAmount() + " " + payment.getCurrency().toUpperCase() +
                                    " via Razorpay was successful for Order ID: " + payment.getOrderId() +
                                    ".\n\nThank you for shopping with us!"
                    );
                } catch (IOException ex) {
                    logger.error("❌ Failed to send Razorpay payment success email for order {}: {}", orderId, ex.getMessage());
                }



                paymentEventPublisher.publishPaymentSuccess(new PaymentEvent(
                        orderId,
                        "succeeded",
                        payment.getPaymentProvider(),
                        externalPaymentId
                ));
            } else {
                logger.warn("No Payment record found for Razorpay orderId: {}", orderId);
            }
        } catch (Exception e) {
            logger.error("Error updating Razorpay payment for orderId {}: {}", orderId, e.getMessage());

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    orderId,
                    payment != null ? payment.getUserId() : "unknown",
                    "razorpay",
                    "Exception during Razorpay processing: " + e.getMessage(),
                    Instant.now()
            );
            paymentEventPublisher.publishPaymentFailedEvent(failedEvent);
        }
        finally {
            MDC.clear();
        }
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByUserId(String userId) {
        String cacheKey = USER_PAYMENT_CACHE_PREFIX + userId;

        List<PaymentResponseDto> cached = (List<PaymentResponseDto>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.info("✅ Redis cache hit for user payments");
            return cached;
        }

        logger.info("🔍 Redis cache miss. Fetching payments from DB...");
        List<Payment> payments = paymentRepository.findByUserId(userId);
        List<PaymentResponseDto> response = payments.stream()
                .map(this::mapToResponseDto)
                .toList();

        // Set cache with TTL
        redisTemplate.opsForValue().set(cacheKey, response, userPaymentCacheTtl, TimeUnit.SECONDS);
        logger.info("📦 Payments cached in Redis with TTL {}s", userPaymentCacheTtl);

        return response;
    }


    private PaymentResponseDto mapToResponseDto(Payment p) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(p.getOrderId());
        dto.setPaymentId(p.getExternalPaymentId());
        dto.setStatus(p.getStatus().name());
        dto.setAmount(p.getAmount() != null ? BigDecimal.valueOf(p.getAmount()) : null);

        dto.setCurrency(p.getCurrency());
        dto.setProvider(p.getPaymentProvider());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
