package org.example.paymentservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.services.PaymentProcessingService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "Payment API", description = "Handles payment creation and status checks")
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Operation(summary = "Create payment link",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentRequestDto.class),
                            examples = @ExampleObject(
                                    value = "{\"amount\": 5000, \"currency\": \"inr\", \"paymentMethodId\": \"pm_123\", \"orderId\": \"order_abc\", \"gateway\": \"stripe\"}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment link created",
                            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping("/link")
    public ResponseEntity<PaymentResponseDto> createPaymentLink(
            @Valid @RequestBody PaymentRequestDto paymentRequest) {

        MDC.put("orderId", paymentRequest.getOrderId());
        MDC.put("paymentProvider", paymentRequest.getGateway());
        PaymentResponseDto response = paymentProcessingService.createPaymentLink(paymentRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Razorpay success redirect", responses = {
            @ApiResponse(responseCode = "200", description = "Success message")
    })
    @GetMapping("/razorpay-success")
    public ResponseEntity<String> razorpaySuccess() {
        return ResponseEntity.ok("Payment was successful (Razorpay). Thank you!");
    }

    @Operation(
            summary = "Get Payment by ID",
            description = "Fetches the complete Payment object using its unique database ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Payment.class),
                                    examples = @ExampleObject(
                                            value = """
                        {
                          "id": 1,
                          "orderId": "ORD123",
                          "userId": "USR456",
                          "paymentProvider": "stripe",
                          "amount": 9999,
                          "currency": "INR",
                          "status": "succeeded",
                          "externalPaymentId": "pi_abc123",
                          "createdAt": "2025-05-05T12:00:00",
                          "updatedAt": "2025-05-05T12:00:01"
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Payment not found")
            }
    )
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long paymentId) {
        Optional<Payment> payment = paymentRepository.findById(paymentId);
        return payment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get Payment Status by Order ID",
            description = "Fetches the payment status (succeeded/pending/failed) using orderId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment status retrieved",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "\"succeeded\"")
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Payment not found")
            }
    )
    @GetMapping("/status/order/{orderId}")
    public ResponseEntity<?> getPaymentStatusByOrderId(@PathVariable String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        return payment != null ? ResponseEntity.ok(payment.getStatus()) : ResponseEntity.notFound().build();
    }
}
