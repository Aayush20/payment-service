package org.example.paymentservice.adapters.razorpay;

import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentGateway implements PaymentGateway {

    @Autowired
    private RazorpayService razorpayService;

    @Override
    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        String orderId = paymentRequest.getOrderId();
        long amount = paymentRequest.getAmount();
        String currency = paymentRequest.getCurrency();

        try {
            String paymentLink = razorpayService.createPaymentLink(orderId, amount, currency);

            if (paymentLink == null) {
                return new PaymentResponseDto(null, null, "FAILED", "Could not generate Razorpay link", null, null, "razorpay", null);
            }

            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentLink);
            responseDto.setStatus("LINK_CREATED");
            responseDto.setMessage("Razorpay link generated successfully");
            responseDto.setProvider("razorpay");

            return responseDto;
        } catch (Exception e) {
            return new PaymentResponseDto(null, null, "FAILED", "Exception: " + e.getMessage(), null, null, "razorpay", null);
        }
    }
}
