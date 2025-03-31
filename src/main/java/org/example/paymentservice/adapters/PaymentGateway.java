package org.example.paymentservice.adapters;

import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;

public interface PaymentGateway {
    /**
     * Creates a hosted payment link for the given payment details.
     * @param paymentRequest the details needed to create the link.
     * @return a response DTO containing the payment link (or session link) and status.
     */
    PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest);
}

