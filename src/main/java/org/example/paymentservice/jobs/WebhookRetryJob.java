package org.example.paymentservice.jobs;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.models.WebhookRetryTask;
import org.example.paymentservice.repositories.WebhookRetryTaskRepository;
import org.example.paymentservice.services.PaymentStatusService;
import org.example.paymentservice.utils.RazorpayWebhookUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebhookRetryJob {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRetryJob.class);
    private final WebhookRetryTaskRepository retryRepo;
    private final PaymentStatusService statusService;

    // Inject via @Value
    private final String stripeSecret = "your_stripe_secret";
    private final String razorpaySecret = "your_razorpay_secret";

    public WebhookRetryJob(WebhookRetryTaskRepository retryRepo, PaymentStatusService statusService) {
        this.retryRepo = retryRepo;
        this.statusService = statusService;
    }

    @Scheduled(fixedDelay = 60000) // Retry every 60 seconds
    public void retryFailedWebhooks() {
        List<WebhookRetryTask> tasks = retryRepo.findByProcessedFalse();

        for (WebhookRetryTask task : tasks) {
            try {
                if (task.getProvider().equalsIgnoreCase("stripe")) {
                    Event event = Webhook.constructEvent(task.getPayload(), task.getSignature(), stripeSecret);
                    statusService.handleStripeCheckoutSessionCompleted(event);
                } else if (task.getProvider().equalsIgnoreCase("razorpay")) {
                    boolean valid = RazorpayWebhookUtils.verifyWebhookSignature(task.getPayload(), task.getSignature(), razorpaySecret);
                    if (!valid) throw new RuntimeException("Invalid Razorpay Signature");
                    statusService.handleRazorpayEvent(new JSONObject(task.getPayload()));
                }

                task.setProcessed(true);
            } catch (Exception e) {
                logger.error("Retry failed for webhook task id {}: {}", task.getId(), e.getMessage());
            } finally {
                task.setAttemptCount(task.getAttemptCount() + 1);
                task.setUpdatedAt(LocalDateTime.now());
                retryRepo.save(task);
            }
        }
    }
}
