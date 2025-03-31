package org.example.paymentservice.services;
import com.stripe.model.Event;
import org.json.JSONObject;

public interface PaymentStatusService {

    /**
     * Update payment status based on a completed Stripe Checkout session.
     */
    void handleStripeCheckoutSessionCompleted(Event event);

    /**
     * Update payment status based on a Razorpay webhook event.
     */
    void handleRazorpayEvent(JSONObject eventPayload);
}

