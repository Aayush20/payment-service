package org.example.paymentservice.adapters.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentGateway implements PaymentGateway {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.success.url}")
    private String stripeSuccessUrl;

    @Value("${stripe.cancel.url}")
    private String stripeCancelUrl;

    @PostConstruct
    public void init() {
        // Set the API key globally
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        try {
            // Build a Checkout Session creation request
            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Order " + paymentRequest.getOrderId())
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(paymentRequest.getCurrency())
                            .setUnitAmount(paymentRequest.getAmount()) // amount in smallest currency unit
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(priceData)
                            .build();

            // Build the session parameters
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeSuccessUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeCancelUrl)
                    .addLineItem(lineItem)
                    .putMetadata("orderId", paymentRequest.getOrderId())
                    .build();

            // Create the session
            Session session = Session.create(params);

            // Return session's URL as the payment link and use session id as external payment id
            return new PaymentResponseDto(session.getUrl(), "link_created", "Stripe payment link generated successfully");

        } catch (StripeException e) {
            e.printStackTrace();
            return new PaymentResponseDto(null, "failed", "Stripe exception: " + e.getMessage());
        }
    }
}