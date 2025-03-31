package org.example.paymentservice.adapters;

import org.example.paymentservice.adapters.razorpay.RazorpayPaymentGateway;
import org.example.paymentservice.adapters.stripe.StripePaymentGateway;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactory {

    private final StripePaymentGateway stripePaymentGateway;
    private final RazorpayPaymentGateway razorpayPaymentGateway;

    @Autowired
    public PaymentGatewayFactory(StripePaymentGateway stripePaymentGateway,
                                 RazorpayPaymentGateway razorpayPaymentGateway) {
        this.stripePaymentGateway = stripePaymentGateway;
        this.razorpayPaymentGateway = razorpayPaymentGateway;
    }

    /**
     * Returns the proper PaymentGateway implementation based on the gateway string.
     *
     * Additional selection logic (such as based on load or traffic) can be added here.
     */
    public PaymentGateway getPaymentGateway(PaymentRequestDto paymentRequest) {
        String gateway = paymentRequest.getGateway();
        if ("stripe".equalsIgnoreCase(gateway)) {
            return stripePaymentGateway;
        } else if ("razorpay".equalsIgnoreCase(gateway)) {
            return razorpayPaymentGateway;
        } else {
            throw new IllegalArgumentException("Invalid payment gateway: " + gateway);
        }
    }
}

