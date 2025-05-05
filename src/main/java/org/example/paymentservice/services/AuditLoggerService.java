package org.example.paymentservice.services;

import org.example.paymentservice.models.PaymentAuditLog;
import org.example.paymentservice.repositories.PaymentAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLoggerService {

    @Autowired
    private PaymentAuditLogRepository auditLogRepository;

    public void log(String orderId, String userId, String provider,
                    Long amount, String currency, String externalPaymentId,
                    String action, String details) {

        PaymentAuditLog log = new PaymentAuditLog();
        log.setOrderId(orderId);
        log.setUserId(userId);
        log.setProvider(provider);
        log.setAmount(amount);
        log.setCurrency(currency);
        log.setExternalPaymentId(externalPaymentId);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(details);

        auditLogRepository.save(log);
    }
}
