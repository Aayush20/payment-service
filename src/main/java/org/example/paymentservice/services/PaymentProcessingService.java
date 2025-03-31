package org.example.paymentservice.services;

import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.adapters.PaymentGatewayFactory;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;

import org.example.paymentservice.models.Payment;
import org.example.paymentservice.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentProcessingService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentGatewayFactory paymentGatewayFactory;

    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        // 1. Create and persist a Payment record.
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency());
        payment.setPaymentProvider(paymentRequest.getGateway());
        payment.setStatus("link_created");
        paymentRepository.save(payment);

        // 2. Use the factory to select the appropriate PaymentGateway implementation.
        PaymentGateway paymentGateway = paymentGatewayFactory.getPaymentGateway(paymentRequest);
        PaymentResponseDto response = paymentGateway.createPaymentLink(paymentRequest);

        // 3. Update the Payment record with data from the gateway response.
        payment.setExternalPaymentId(response.getPaymentId());
        payment.setStatus(response.getStatus());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return response;
    }
}
