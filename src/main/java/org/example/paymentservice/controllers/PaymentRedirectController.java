package org.example.paymentservice.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@Controller
public class PaymentRedirectController {

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

    @GetMapping("/api/payment/razorpay-cancel")
    @ResponseBody
    public String razorpayCancel() {
        return "Payment was cancelled.";
    }

    @GetMapping("/api/payment/success")
    @ResponseBody
    public String stripeSuccess() {
        return "Payment was successful (Stripe).";
    }

    @GetMapping("/api/payment/cancel")
    @ResponseBody
    public String stripeCancel() {
        return "Payment was cancelled (Stripe).";
    }

}

