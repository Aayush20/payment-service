package org.example.paymentservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.paymentservice.dtos.PaymentRequestDto;
import org.example.paymentservice.dtos.PaymentResponseDto;
import org.example.paymentservice.dtos.TokenIntrospectionResponseDTO;
import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentStatus;
import org.example.paymentservice.repositories.PaymentRepository;
import org.example.paymentservice.security.HasScope;
import org.example.paymentservice.services.PaymentProcessingService;
import org.example.paymentservice.services.PaymentStatusService;
import org.example.paymentservice.services.TokenService;
import org.example.paymentservice.utils.TokenClaimUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payment API", description = "Handles payment creation and status checks")
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired private PaymentProcessingService paymentProcessingService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private TokenService tokenService;
    @Autowired private PaymentStatusService paymentService;

    @Operation(
            summary = "Create payment link",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentRequestDto.class),
                            examples = @ExampleObject(
                                    name = "Sample Payment Request",
                                    value = """
                    {
                      "amount": 5000,
                      "currency": "INR",
                      "paymentMethodId": "pm_123",
                      "orderId": "order_abc",
                      "gateway": "stripe"
                    }
                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment link created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaymentResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "Payment Response",
                                            value = """
                        {
                          "paymentId": "pay_abc123",
                          "status": "succeeded",
                          "message": "Payment completed successfully"
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request data",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Validation Error",
                                            value = """
                        {
                          "timestamp": "2025-05-14T14:30:00",
                          "status": 400,
                          "error": "Validation Failed",
                          "message": "Amount should be at least 1",
                          "path": "/api/payment/link"
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/link")
    @HasScope("payment:create")
    public ResponseEntity<PaymentResponseDto> createPaymentLink(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            @RequestHeader("Authorization") String authHeader) {
        MDC.put("orderId", paymentRequest.getOrderId());
        MDC.put("paymentProvider", paymentRequest.getGateway());
        PaymentResponseDto response = paymentProcessingService.createPaymentLink(paymentRequest, authHeader);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Razorpay success redirect",
            description = "Returns static text after Razorpay success redirect",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success message")
            }
    )
    @GetMapping("/razorpay-success")
    public ResponseEntity<String> razorpaySuccess() {
        return ResponseEntity.ok("Payment was successful (Razorpay). Thank you!");
    }

    @Operation(
            summary = "Get Payment by ID",
            description = "Fetch the payment record by internal ID",
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
    @HasScope("payment:read")
    public ResponseEntity<?> getPaymentById(@PathVariable Long paymentId) {
        Optional<Payment> payment = paymentRepository.findById(paymentId);
        return payment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get Payment Status by Order ID",
            description = "Returns just the payment status string by order ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status returned",
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(value = "\"succeeded\"")
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Order ID not found")
            }
    )
    @GetMapping("/status/order/{orderId}")
    @HasScope("payment:read")
    public ResponseEntity<?> getPaymentStatusByOrderId(@PathVariable String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        return payment != null ? ResponseEntity.ok(payment.getStatus()) : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Rollback payment for order (internal use only)",
            description = "Called by order-service to rollback payment on order cancel or failure",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rollback acknowledged"),
                    @ApiResponse(responseCode = "403", description = "Access denied")
            }
    )
    @PostMapping("/internal/rollback")
    public ResponseEntity<String> rollbackPayment(@RequestParam String orderId,
                                                  @RequestHeader("Authorization") String tokenHeader) {
        TokenIntrospectionResponseDTO token = tokenService.introspect(tokenHeader);
        if (!TokenClaimUtils.hasScope(token, "internal") && !TokenClaimUtils.isSystemCall(token, "order-service")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        // TODO: Optional logic to mark payment as ROLLED_BACK, if applicable
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment != null && !payment.getStatus().equals(PaymentStatus.SUCCEEDED)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        return ResponseEntity.ok("Payment rollback acknowledged for order: " + orderId);
    }


    @Operation(summary = "Get authenticated user's payment history")
    @GetMapping("/me/payments")
    @HasScope("payment:read")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(@RequestHeader("Authorization") String tokenHeader) {
        TokenIntrospectionResponseDTO token = tokenService.introspect(tokenHeader);
        List<PaymentResponseDto> payments = paymentService.getPaymentsByUserId(token.getSub());
        return ResponseEntity.ok(payments);
    }



}
