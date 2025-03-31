package org.example.paymentservice.controllers;

import jakarta.validation.Valid;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.services.PaymentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentProcessingService paymentProcessingService;

//    @PostMapping("/process")
//    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequest) {
//        PaymentResponseDto response = paymentProcessingService.processPayment(paymentRequest);
//        return new ResponseEntity<>(response, HttpStatus.OK);
//    }

    /**
     * Payment link creation endpoint.
     * The client sends minimal order/amount details and the server creates
     * a hosted payment link (e.g., Stripe Checkout URL or Razorpay Payment Page URL)
     * for the client to complete the payment.
     */
    @PostMapping("/link")
    public ResponseEntity<PaymentResponseDto> createPaymentLink(@Valid @RequestBody PaymentRequestDto paymentRequest) {
        PaymentResponseDto response = paymentProcessingService.createPaymentLink(paymentRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Option 1: Return a simple text message:
    @GetMapping("/api/payment/razorpay-success")
    @ResponseBody
    public String razorpaySuccess() {
        return "Payment was successful (Razorpay). Thank you!";
    }

    // Option 2: Redirect to a static HTML page or homepage
    // @GetMapping("/api/payment/razorpay-success")
    // public String razorpaySuccessRedirect() {
    //     // For example, if you have a static HTML file at /public/success.html
    //     return "redirect:/success.html";
    // }
}


