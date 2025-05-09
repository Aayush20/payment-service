package org.example.paymentservice.services;

import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.adapters.PaymentGatewayFactory;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentAuditLog;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentAuditLogRepository;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.utils.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentGatewayFactory paymentGatewayFactory;

    @Autowired
    private PaymentAuditLogRepository auditLogRepository;

    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        String userId = AuthUtils.getCurrentUserId();
        String orderId = paymentRequest.getOrderId();
        String provider = paymentRequest.getGateway();

        // Set MDC values for structured logging
        MDC.put("orderId", orderId);
        MDC.put("userId", userId);
        MDC.put("provider", provider);
        MDC.put("correlationId", UUID.randomUUID().toString());


        try {
            logger.info("Starting payment link creation for orderId: {}", orderId);

            // Step 1: Create and persist a Payment record
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(paymentRequest.getAmount());
            payment.setCurrency(paymentRequest.getCurrency());
            payment.setPaymentProvider(provider);
            payment.setStatus(PaymentStatus.LINK_CREATED);

            payment.setUserId(userId);

            paymentRepository.save(payment);
            logger.info("Initial payment record saved for orderId: {}", orderId);

            // Step 2: Select appropriate PaymentGateway implementation
            PaymentGateway paymentGateway = paymentGatewayFactory.getPaymentGateway(paymentRequest);
            PaymentResponseDto response = paymentGateway.createPaymentLink(paymentRequest);

            // Step 3: Update Payment record with external data
            payment.setExternalPaymentId(response.getPaymentId());
            payment.setStatus(PaymentStatus.valueOf(response.getStatus().toUpperCase()));

            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentAuditLog auditLog = new PaymentAuditLog(
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getPaymentProvider(),
                    payment.getAmount(),
                    PaymentStatus.INITIATED.name(),
                    LocalDateTime.now(),
                    "Payment link created"
            );
            auditLogRepository.save(auditLog);


            logger.info("Payment record updated after gateway response for orderId: {}", orderId);
            return response;
        } catch (Exception ex) {
            logger.error("Failed to create payment link for orderId: {}. Error: {}", orderId, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.clear(); // Clear context after operation
        }
    }
}
