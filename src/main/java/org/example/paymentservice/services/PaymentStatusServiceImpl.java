//package org.example.paymentservice.services;
//
//
//import com.stripe.model.Event;
//import com.stripe.model.checkout.Session;
//import org.example.paymentservice.models.Payment;
//import org.example.paymentservice.repositories.PaymentRepository;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class PaymentStatusServiceImpl implements PaymentStatusService {
//
//    @Autowired
//    private PaymentRepository paymentRepository;
//
//    /**
//     * Handles a Stripe webhook event indicating that a Checkout session
//     * has been completed successfully.
//     *
//     * The event is expected to include session metadata with an "orderId".
//     * The method retrieves the Payment record by orderId, updates its status to “succeeded”
//     * and sets the external payment ID (i.e. the Stripe session id).
//     *
//     * @param event the Stripe Event object from the webhook.
//     */
//    @Override
//    public void handleStripeCheckoutSessionCompleted(Event event) {
//        // Attempt to deserialize the event data into a Stripe Checkout Session object.
//        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
//
//        if (session != null) {
//            // Obtain the orderId and external payment ID (session id) from the session metadata.
//            String orderId = session.getMetadata().get("orderId");
//            String externalPaymentId = session.getId();
//
//            // Retrieve the Payment record by orderId (assumes orderId is unique).
//            Payment payment = paymentRepository.findByOrderId(orderId);
//
//            if (payment != null) {
//                // Update the Payment record as succeeded.
//                payment.setStatus("succeeded");
//                payment.setExternalPaymentId(externalPaymentId);
//                paymentRepository.save(payment);
//            } else {
//                // Optionally log: Payment for orderId not found.
//            }
//        } else {
//            // Optionally log: Could not deserialize session from event.
//        }
//    }
//
//
//
//    /**
//     * Handles a Razorpay webhook event.
//     *
//     * This example assumes that the Razorpay event payload contains:
//     * - "event": a string indicating the event type (e.g. "payment_link.paid")
//     * - "payload" containing a "payment_link" JSON object.
//     *    * "reference_id": your custom orderId.
//     *    * "id": the external payment link identifier.
//     *
//     * If the event type is "payment_link.paid", the Payment record with the matching orderId is updated.
//     *
//     * @param eventPayload the JSONObject representing the Razorpay event payload.
//     */
//    @Override
//    public void handleRazorpayEvent(JSONObject eventPayload) {
//        // Extract the event type.
//        String eventType = eventPayload.optString("event");
//        if ("payment_link.paid".equals(eventType)) {
//            JSONObject payload = eventPayload.optJSONObject("payload");
//            if (payload != null) {
//                JSONObject paymentLinkObj = payload.optJSONObject("payment_link");
//                if (paymentLinkObj != null) {
//                    // Retrieve custom reference_id (orderId) and Razorpay's payment link ID.
//                    String orderId = paymentLinkObj.optString("reference_id");
//                    String externalPaymentId = paymentLinkObj.optString("id");
//
//                    // Find and update the Payment record.
//                    Payment payment = paymentRepository.findByOrderId(orderId);
//                    if (payment != null) {
//                        payment.setStatus("succeeded");
//                        payment.setExternalPaymentId(externalPaymentId);
//                        paymentRepository.save(payment);
//                    } else {
//                        // Optionally log: Payment for orderId not found.
//                    }
//                }
//            }
//        }
//        // Optionally, more event types (e.g., payment failures) can be handled here.
//    }
//}
//
package org.example.paymentservice.services;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.repositories.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional; // Uncomment if you want transaction management

import java.util.Optional;

@Service
// @Transactional  // Uncomment this annotation if you wish each handler to run inside a transaction
public class PaymentStatusServiceImpl implements PaymentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Handles a Stripe webhook event indicating that a Checkout session
     * has been completed successfully.
     *
     * The event is expected to include session metadata with an "orderId".
     * The method retrieves the Payment record by orderId, updates its status to “succeeded”
     * and sets the external payment ID (i.e. the Stripe session id).
     *
     * @param event the Stripe Event object from the webhook.
     */
    @Override
    public void handleStripeCheckoutSessionCompleted(Event event) {
        logger.info("Received Stripe event: {}", event);

        // Safely extract the session object
//        Optional<Object> optionalSession = event.getDataObjectDeserializer().getObject();
        Optional<StripeObject> optionalSession =
                event.getDataObjectDeserializer().getObject();
        if (!optionalSession.isPresent()) {
            logger.error("Failed to deserialize session from Stripe event: {}", event);
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
            logger.error("Missing orderId in session metadata. Session: {}", session);
            return;
        }

        String orderId = session.getMetadata().get("orderId").trim();
        if (orderId.isEmpty()) {
            logger.error("Received Stripe event with empty orderId.");
            return;
        }

        String externalPaymentId = session.getId();
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment != null) {
            payment.setStatus("succeeded");
            payment.setExternalPaymentId(externalPaymentId);
            try {
                paymentRepository.save(payment);
                logger.info("Payment updated successfully for order: {} with external id: {}", orderId, externalPaymentId);
            } catch (Exception e) {
                logger.error("Failed to update payment for order {}. Error: {}", orderId, e.getMessage());
            }
        } else {
            logger.warn("No Payment record found for orderId: {}", orderId);
        }
    }

    /**
     * Handles a Razorpay webhook event.
     *
     * This example assumes that the Razorpay event payload contains:
     * - "event": a string indicating the event type (e.g. "payment_link.paid")
     * - "payload" containing a "payment_link" JSON object.
     *    * "reference_id": your custom orderId.
     *    * "id": the external payment link identifier.
     *
     * If the event type is "payment_link.paid", the Payment record with the matching orderId is updated.
     *
     * @param eventPayload the JSONObject representing the Razorpay event payload.
     */
    @Override
    public void handleRazorpayEvent(JSONObject eventPayload) {
        logger.info("Received Razorpay event payload: {}", eventPayload);

        // Extract the event type.
        String eventType = eventPayload.optString("event", "").trim();
        if (!"payment_link.paid".equals(eventType)) {
            logger.warn("Unhandled Razorpay event type: {}", eventType);
            return;
        }

        JSONObject payload = eventPayload.optJSONObject("payload");
        if (payload == null) {
            logger.error("No payload found in Razorpay event: {}", eventPayload);
            return;
        }
        JSONObject paymentLinkObj = payload.optJSONObject("payment_link");
        if (paymentLinkObj == null) {
            logger.error("No payment_link object found in Razorpay event payload: {}", eventPayload);
            return;
        }

        // Retrieve custom reference_id (orderId) and Razorpay's payment link ID.
        String orderId = paymentLinkObj.optString("reference_id", "").trim();
        if (orderId.isEmpty()) {
            logger.error("Razorpay event payload missing 'reference_id'. Payload: {}", paymentLinkObj);
            return;
        }
        String externalPaymentId = paymentLinkObj.optString("id", "").trim();
        if (externalPaymentId.isEmpty()) {
            logger.error("Razorpay event payload missing payment link id. Payload: {}", paymentLinkObj);
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment != null) {
            payment.setStatus("succeeded");
            payment.setExternalPaymentId(externalPaymentId);
            try {
                paymentRepository.save(payment);
                logger.info("Payment updated successfully for Razorpay order: {} with external id: {}", orderId, externalPaymentId);
            } catch (Exception e) {
                logger.error("Failed to update payment for order {}. Error: {}", orderId, e.getMessage());
            }
        } else {
            logger.warn("No Payment record found for orderId: {} in Razorpay event", orderId);
        }
    }
}

