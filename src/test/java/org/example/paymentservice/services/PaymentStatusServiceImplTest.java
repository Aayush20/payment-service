package org.example.paymentservice.services;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import org.example.paymentservice.configs.kafka.PaymentEventPublisher;
import org.example.paymentservice.events.PaymentEvent;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.repositories.PaymentRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest
public class PaymentStatusServiceImplTest {

    @InjectMocks
    private PaymentStatusServiceImpl paymentStatusService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHandleStripeCheckoutSessionCompleted_validEvent() {
        Session session = mock(Session.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", "order123");

        when(session.getMetadata()).thenReturn(metadata);
        when(session.getId()).thenReturn("sess_123");

        Event event = mock(Event.class);
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(session));
        when(session.getMetadata()).thenReturn(metadata);

        Payment payment = new Payment();
        payment.setOrderId("order123");

        when(paymentRepository.findByOrderId("order123")).thenReturn(payment);

        paymentStatusService.handleStripeCheckoutSessionCompleted(event);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventPublisher, times(1)).publishPaymentSuccess(any(PaymentEvent.class));
    }

    @Test
    public void testHandleRazorpayEvent_validPayload() {
        JSONObject payload = new JSONObject();
        payload.put("event", "payment_link.paid");

        JSONObject entity = new JSONObject();
        entity.put("reference_id", "order456");
        entity.put("id", "pay_456");

        JSONObject paymentLink = new JSONObject();
        paymentLink.put("entity", entity);

        JSONObject payloadWrapper = new JSONObject();
        payloadWrapper.put("payment_link", paymentLink);

        payload.put("payload", payloadWrapper);

        Payment payment = new Payment();
        payment.setOrderId("order456");

        when(paymentRepository.findByOrderId("order456")).thenReturn(payment);

        paymentStatusService.handleRazorpayEvent(payload);

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventPublisher, times(1)).publishPaymentSuccess(any(PaymentEvent.class));
    }
}