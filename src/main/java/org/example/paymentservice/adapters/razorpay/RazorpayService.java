package org.example.paymentservice.adapters.razorpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String createPaymentLink(String orderId, long amount, String currency) throws Exception {
        logger.info("➡️ Creating Razorpay payment link for Order: {}", orderId);

        // Simulated API call to Razorpay (replace with real logic)
        if (Math.random() < 0.3) throw new RuntimeException("Razorpay API timeout");

        return "rzp_test_link_" + orderId; // Placeholder link
    }

    @Recover
    public String fallback(Exception ex, String orderId, long amount, String currency) {
        logger.error("🔥 Razorpay link creation failed after retries: {}", ex.getMessage());
        return null;
    }
}
