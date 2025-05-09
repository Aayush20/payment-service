package org.example.paymentservice.jobs;


import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.example.paymentservice.models.WebhookRetryTask;
import org.example.paymentservice.repositories.WebhookRetryTaskRepository;
import org.example.paymentservice.services.PaymentStatusService;
import org.example.paymentservice.utils.RazorpayWebhookUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class WebhookRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRetryScheduler.class);

    private final WebhookRetryTaskRepository retryRepo;
    private final PaymentStatusService paymentStatusService;

    @Value("${stripe.webhook.secret}")
    private String stripeSecret;

    @Value("${razorpay.webhook.secret}")
    private String razorpaySecret;

    @Value("${feature.retry.enabled:true}")
    private boolean isRetryEnabled;

    public WebhookRetryScheduler(WebhookRetryTaskRepository retryRepo,
                                 PaymentStatusService paymentStatusService) {
        this.retryRepo = retryRepo;
        this.paymentStatusService = paymentStatusService;
    }

    @Scheduled(fixedDelay = 30000) // Runs every 30 seconds
    public void retryWebhookTasks() {
        if (!isRetryEnabled) {
            logger.info("Retry feature disabled via config.");
            return;
        }
        List<WebhookRetryTask> tasks = retryRepo.findTop10ByProcessedFalseOrderByCreatedAtAsc();
        for (WebhookRetryTask task : tasks) {
            try {
                if ("stripe".equalsIgnoreCase(task.getProvider())) {
                    Event event = Webhook.constructEvent(
                            task.getPayload(), task.getSignature(), stripeSecret
                    );
                    paymentStatusService.handleStripeCheckoutSessionCompleted(event);
                } else if ("razorpay".equalsIgnoreCase(task.getProvider())) {
                    boolean valid = RazorpayWebhookUtils.verifyWebhookSignature(
                            task.getPayload(), task.getSignature(), razorpaySecret
                    );
                    if (!valid) throw new IllegalArgumentException("Invalid Razorpay signature");
                    JSONObject payloadJson = new JSONObject(task.getPayload());
                    paymentStatusService.handleRazorpayEvent(payloadJson);
                }

                task.setProcessed(true);
                logger.info("✅ Successfully retried webhook task ID {}", task.getId());
            } catch (Exception ex) {
                logger.warn("⚠️ Retry failed for task ID {}: {}", task.getId(), ex.getMessage());
            }

            task.setAttemptCount(task.getAttemptCount() + 1);
            task.setUpdatedAt(LocalDateTime.now());
            retryRepo.save(task);
        }
    }
}

