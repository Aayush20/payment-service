package org.example.paymentservice.adapters.razorpay;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.PaymentLink;
import org.example.paymentservice.adapters.PaymentGateway;
import org.json.JSONObject;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentGateway implements PaymentGateway {

    // The Razorpay client is injected as a bean.
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
            // Razorpay expects amount in the smallest currency unit (e.g., INR paise).
            options.put("amount", paymentRequest.getAmount());
            options.put("currency", paymentRequest.getCurrency());
            options.put("accept_partial", false);
            options.put("description", "Payment for order " + paymentRequest.getOrderId());
            options.put("reference_id", paymentRequest.getOrderId());
            options.put("callback_url", razorpaySuccessUrl);
            options.put("callback_method", "get");

            // Create the payment link via Razorpay's API.
            PaymentLink paymentLink = razorpayClient.paymentLink.create(options);

            String linkUrl = paymentLink.get("short_url");
            return new PaymentResponseDto(linkUrl, "link_created", "Razorpay payment link generated successfully");

        } catch (RazorpayException e) {
            e.printStackTrace();
            return new PaymentResponseDto(null, "failed", "Razorpay exception: " + e.getMessage());
        }
    }
}
