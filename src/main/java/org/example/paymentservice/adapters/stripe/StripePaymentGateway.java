package org.example.paymentservice.adapters.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.example.paymentservice.adapters.PaymentGateway;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private StripeService stripeService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentResponseDto createPaymentLink(PaymentRequestDto paymentRequest) {
        try {
            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Order " + paymentRequest.getOrderId())
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(paymentRequest.getCurrency())
                            .setUnitAmount(paymentRequest.getAmount()) // in smallest currency unit
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(priceData)
                            .build();

            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeSuccessUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeCancelUrl)
                    .addLineItem(lineItem)
                    .putMetadata("orderId", paymentRequest.getOrderId())
                    .build();

            Session session = stripeService.createSession(params);

            PaymentResponseDto responseDto = new PaymentResponseDto();
            responseDto.setPaymentId(session.getId());
            responseDto.setStatus("LINK_CREATED");
            responseDto.setMessage("Stripe payment link generated successfully");
            return responseDto;

        } catch (StripeException e) {
            return new PaymentResponseDto(null, null, "FAILED", "Stripe exception: " + e.getMessage(), null, null, "stripe", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
