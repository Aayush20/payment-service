package org.example.paymentservice.adapters.razorpay;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.PaymentLink;
import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.success.url}")
    private String razorpaySuccessUrl;

    public RazorpayPaymentGateway(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    @Override
    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", paymentRequest.getAmount());
            options.put("currency", paymentRequest.getCurrency());
            options.put("accept_partial", false);
            options.put("description", "Payment for order " + paymentRequest.getOrderId());
            options.put("reference_id", paymentRequest.getOrderId());
            options.put("callback_url", razorpaySuccessUrl);
            options.put("callback_method", "get");

            PaymentLink paymentLink = razorpayClient.paymentLink.create(options);
            String linkUrl = paymentLink.get("short_url");

            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(paymentLink.get("id")); // Razorpay PaymentLink ID
            responseDto.setStatus("LINK_CREATED");
            responseDto.setMessage("Razorpay payment link generated successfully");
            return responseDto;

        } catch (RazorpayException e) {
            return new PaymentResponseDto(null, null, "FAILED", "Razorpay exception: " + e.getMessage(), null, null, "razorpay", null);
        }
    }
}
