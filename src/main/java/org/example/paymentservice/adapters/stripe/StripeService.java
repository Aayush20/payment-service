package org.example.paymentservice.adapters.stripe;


import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Session createSession(SessionCreateParams params) throws Exception {
        Stripe.apiKey = stripeApiKey;
        log.info("➡️ Creating Stripe Checkout Session...");
        return Session.create(params);
    }

    @Recover
    public Session fallback(Exception ex, SessionCreateParams params) {
        log.error("🔥 Stripe session creation failed after retries: {}", ex.getMessage());
        return null; // You may choose to throw a custom exception
    }
}

