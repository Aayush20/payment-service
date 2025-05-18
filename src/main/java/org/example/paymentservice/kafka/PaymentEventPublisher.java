package org.example.paymentservice.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topic.payment.success}")
    private String successTopic;

    @Value("${topic.payment.failed}")
    private String failedTopic;

    @Value("${topic.payment.retry:payment.retry}")
    private String retryTopic;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentSuccess(PaymentEvent event) {
        try {
            logger.info("✅ Publishing to topic {}: {}", successTopic, event);
            kafkaTemplate.send(successTopic, event.getOrderId(), event);
        } catch (Exception ex) {
            logger.error("❌ Failed to publish payment.success for order {}: {}. Redirecting to retry topic.", event.getOrderId(), ex.getMessage());
            sendToRetryTopic(event.getOrderId(), event);
        }
    }

    public void publishPaymentFailedEvent(PaymentFailedEvent event) {
        try {
            logger.info("❌ Publishing to topic {}: {}", failedTopic, event);
            kafkaTemplate.send(failedTopic, event.getOrderId(), event);
        } catch (Exception ex) {
            logger.error("🔥 Failed to publish payment.failed for order {}: {}. Redirecting to retry topic.", event.getOrderId(), ex.getMessage());
            sendToRetryTopic(event.getOrderId(), event);
        }
    }

    private void sendToRetryTopic(String key, Object event) {
        try {
            logger.info("📦 Sending to retry topic: {}", retryTopic);
            kafkaTemplate.send(new ProducerRecord<>(retryTopic, key, event));
        } catch (Exception ex) {
            logger.error("🚨 Failed to send to retry topic as well: {}", ex.getMessage());
            // Optional: persist to DB for manual replay or alert
        }
    }
}
