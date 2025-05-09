package org.example.paymentservice.jobs;

import jakarta.transaction.Transactional;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.services.AuditLoggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentExpiryScheduler {
    @Value("${feature.payment.expiry.enabled:true}")
    private boolean isExpiryEnabled;

    private static final Logger logger = LoggerFactory.getLogger(PaymentExpiryScheduler.class);
    private final PaymentRepository paymentRepository;
    private final AuditLoggerService auditLoggerService;

    public PaymentExpiryScheduler(PaymentRepository paymentRepository,
                                  AuditLoggerService auditLoggerService) {
        this.paymentRepository = paymentRepository;
        this.auditLoggerService = auditLoggerService;
    }

    @Scheduled(fixedRate = 600000) // every 10 minutes
    @Transactional
    public void expireStalePayments() {
        if (!isExpiryEnabled) {
            logger.info("Payment expiry feature disabled via config.");
            return;
        }
        LocalDateTime expiryCutoff = LocalDateTime.now().minusMinutes(15);
        List<Payment> stalePayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.INITIATED, expiryCutoff);

        if (stalePayments.isEmpty()) {
            logger.info("No stale INITIATED payments found for expiry.");
            return;
        }

        for (Payment payment : stalePayments) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            auditLoggerService.log(
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getPaymentProvider(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getExternalPaymentId(),
                    PaymentStatus.FAILED.name(),
                    "Auto-expired after 15 mins"
            );


            logger.info("Expired INITIATED payment: id={}, orderId={}", payment.getId(), payment.getOrderId());
        }
    }
}
