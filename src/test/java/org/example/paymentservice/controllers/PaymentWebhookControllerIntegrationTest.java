package org.example.paymentservice.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testStripeWebhook_BadSignature() throws Exception {
        mockMvc.perform(post("/api/payment/webhook/stripe")
                        .header("Stripe-Signature", "invalid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_123\",\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRazorpayWebhook_InvalidSignature() throws Exception {
        mockMvc.perform(post("/api/payment/webhook/razorpay")
                        .header("X-Razorpay-Signature", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment_link.paid\",\"payload\":{\"payment_link\":{\"entity\":{\"reference_id\":\"order123\",\"id\":\"pay_123\"}}}}"))
                .andExpect(status().isBadRequest());
    }
}
