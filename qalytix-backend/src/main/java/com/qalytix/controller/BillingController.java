package com.qalytix.controller;

import com.qalytix.dto.request.CreateCheckoutRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.BillingResponse;
import com.qalytix.dto.response.CheckoutResponse;
import com.qalytix.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** GET /api/v1/billing/plan — current plan, status, usage meters */
    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<BillingResponse>> getPlan() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getCurrentBilling()));
    }

    /** POST /api/v1/billing/checkout — create Stripe Checkout session */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CreateCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.createCheckoutSession(request)));
    }

    /** POST /api/v1/billing/portal — create Stripe Billing Portal session */
    @PostMapping("/portal")
    public ResponseEntity<ApiResponse<CheckoutResponse>> portal() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.createPortalSession()));
    }
}
