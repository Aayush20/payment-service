package org.example.paymentservice.services;

import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.adapters.PaymentGatewayFactory;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentAuditLog;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentAuditLogRepository;
import org.example.paymentservice.repositories.PaymentRepository;
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

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentGatewayFactory paymentGatewayFactory;
    @Autowired private PaymentAuditLogRepository auditLogRepository;
    @Autowired private TokenService tokenService;

    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest, String tokenHeader) {
        TokenIntrospectionResponseDTO token = tokenService.introspect(tokenHeader);
        String userId = token.getSub();
        String userEmail = token.getEmail(); // ✅ New
        String orderId = paymentRequest.getOrderId();
        String provider = paymentRequest.getGateway();

        MDC.put("orderId", orderId);
        MDC.put("userId", userId);
        MDC.put("provider", provider);
        MDC.put("correlationId", UUID.randomUUID().toString());

        try {
            logger.info("Starting payment link creation for orderId: {}", orderId);

            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setUserId(userId);
            payment.setUserEmail(userEmail); // ✅ New
            payment.setAmount(paymentRequest.getAmount());
            payment.setCurrency(paymentRequest.getCurrency());
            payment.setPaymentProvider(provider);
            payment.setStatus(PaymentStatus.LINK_CREATED);

            paymentRepository.save(payment);

            PaymentGateway gateway = paymentGatewayFactory.getPaymentGateway(paymentRequest);
            PaymentResponseDto response = gateway.createPaymentLink(paymentRequest);

            payment.setExternalPaymentId(response.getPaymentId());
            payment.setStatus(PaymentStatus.valueOf(response.getStatus().toUpperCase()));
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentAuditLog log = new PaymentAuditLog(
                    payment.getOrderId(), userId, provider, payment.getAmount(),
                    PaymentStatus.INITIATED.name(), LocalDateTime.now(), "Payment link created"
            );
            auditLogRepository.save(log);

            return response;
        } catch (Exception ex) {
            logger.error("Failed to create payment link for orderId: {}. Error: {}", orderId, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
