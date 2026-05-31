package com.qalytix.service;

import com.qalytix.dto.request.CreateCheckoutRequest;
import com.qalytix.dto.response.BillingResponse;
import com.qalytix.dto.response.CheckoutResponse;

public interface BillingService {

    /** Returns the current plan, status and usage metrics for the caller's org. */
    BillingResponse getCurrentBilling();

    /** Creates a Stripe Checkout session to upgrade/change plan. */
    CheckoutResponse createCheckoutSession(CreateCheckoutRequest request);

    /** Creates a Stripe Billing Portal session for self-service subscription management. */
    CheckoutResponse createPortalSession();
}
