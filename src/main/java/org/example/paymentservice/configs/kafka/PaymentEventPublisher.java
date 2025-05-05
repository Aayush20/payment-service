package org.example.paymentservice.configs.kafka;

import org.example.paymentservice.events.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${topic.payment.success}")
    private String successTopic;

    @Value("${topic.payment.failed}")
    private String failedTopic;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentSuccess(PaymentEvent event) {
        logger.info("Publishing to topic {}: {}", successTopic, event);
        kafkaTemplate.send(successTopic, event.getOrderId(), event);
    }

    public void publishPaymentFailure(PaymentEvent event) {
        logger.info("Publishing to topic {}: {}", failedTopic, event);
        kafkaTemplate.send(failedTopic, event.getOrderId(), event);
    }
}

