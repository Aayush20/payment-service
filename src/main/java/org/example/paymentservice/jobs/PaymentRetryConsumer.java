package org.example.paymentservice.jobs;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.paymentservice.kafka.PaymentEvent;
import org.example.paymentservice.kafka.PaymentEventPublisher;
import org.example.paymentservice.kafka.PaymentFailedEvent;
import org.example.paymentservice.models.RetryDeadLetterLog;
import org.example.paymentservice.repositories.RetryDeadLetterLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentRetryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryConsumer.class);
    private final PaymentEventPublisher publisher;
    @Autowired private RetryDeadLetterLogRepository deadLetterRepo;

    public PaymentRetryConsumer(PaymentEventPublisher publisher) {
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${topic.payment.retry}", groupId = "payment-retry-consumer")
    public void retryFailedEvent(ConsumerRecord<String, Object> record) {
        String orderId = record.key();
        Object value = record.value();

        logger.info("🔁 Consuming retry event for orderId={}, value={}", orderId, value);

        try {
            if (value instanceof PaymentEvent event) {
                publisher.publishPaymentSuccess(event);
            } else if (value instanceof PaymentFailedEvent failedEvent) {
                publisher.publishPaymentFailedEvent(failedEvent);
            } else {
                logger.warn("⚠️ Unknown object type in retry topic: {}", value.getClass().getName());
            }
        } catch (Exception ex) {
            logger.error("🔥 Retry from retry topic failed again for orderId={}: {}", orderId, ex.getMessage());
            RetryDeadLetterLog log = new RetryDeadLetterLog();
            log.setTopic(record.topic());
            log.setKey(orderId);
            log.setPayload(value.toString());
            log.setErrorMessage(ex.getMessage());
            log.setCreatedAt(LocalDateTime.now());
            deadLetterRepo.save(log);
        }
    }
}
