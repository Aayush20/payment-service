package org.example.paymentservice.services;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.example.paymentservice.configs.kafka.PaymentEventPublisher;
import org.example.paymentservice.events.PaymentEvent;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentAuditLog;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentAuditLogRepository;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.utils.AuthUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentStatusServiceImpl implements PaymentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private PaymentAuditLogRepository paymentAuditLogRepository;

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

        // Set MDC context for logging
        MDC.put("orderId", orderId);
        MDC.put("provider", "stripe");
        MDC.put("externalPaymentId", externalPaymentId);
        MDC.put("traceId", UUID.randomUUID().toString());
        MDC.put("userId", AuthUtils.getCurrentUserId());


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

                paymentEventPublisher.publishPaymentSuccess(new PaymentEvent(
                        orderId,
                        "succeeded",
                        payment.getPaymentProvider(),
                        externalPaymentId
                ));
            } else {
                logger.warn("No Payment record found for orderId: {}", orderId);
            }
        } catch (Exception e) {
            logger.error("Error updating payment for orderId {}: {}", orderId, e.getMessage());
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

        // Set MDC context for logging
        MDC.put("orderId", orderId);
        MDC.put("provider", "razorpay");
        MDC.put("externalPaymentId", externalPaymentId);
        MDC.put("traceId", UUID.randomUUID().toString());
        MDC.put("userId", AuthUtils.getCurrentUserId());

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
        } finally {
            MDC.clear();
        }
    }
}
