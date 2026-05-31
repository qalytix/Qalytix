package com.qalytix.integration.stripe;

import com.qalytix.entity.enums.Plan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stripe integration stub.
 *
 * All methods that would call the Stripe SDK are marked with
 * TODO: STRIPE — replace with real SDK calls once keys are configured.
 *
 * To activate:
 *  1. Add stripe-java to pom.xml
 *  2. Set STRIPE_SECRET_KEY and STRIPE_WEBHOOK_SECRET env vars
 *  3. Replace each TODO block with the real Stripe SDK call
 */
@Slf4j
@Service
public class StripeService {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    public boolean isConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    /**
     * Creates (or retrieves) a Stripe Customer for the given org.
     *
     * @return Stripe customer ID (e.g. "cus_xxx")
     */
    public String createOrGetCustomer(Long orgId, String orgName, String ownerEmail) {
        if (!isConfigured()) {
            // TODO: STRIPE — Stripe.apiKey = stripeSecretKey;
            // CustomerCreateParams params = CustomerCreateParams.builder()
            //         .setName(orgName)
            //         .setEmail(ownerEmail)
            //         .putMetadata("org_id", orgId.toString())
            //         .build();
            // return Customer.create(params).getId();
            log.warn("[StripeService] Stripe not configured — returning mock customer ID for org {}", orgId);
            return "cus_mock_" + orgId;
        }
        // TODO: STRIPE — real implementation goes here
        return "cus_mock_" + orgId;
    }

    /**
     * Creates a Stripe Checkout Session for upgrading to the given plan.
     *
     * @return Checkout session URL to redirect the user to
     */
    public String createCheckoutSession(String customerId, Plan plan, BillingPeriodParam period,
                                        String successUrl, String cancelUrl) {
        if (!isConfigured()) {
            // TODO: STRIPE — Stripe.apiKey = stripeSecretKey;
            // String priceId = resolvePriceId(plan, period);
            // SessionCreateParams params = SessionCreateParams.builder()
            //         .setCustomer(customerId)
            //         .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            //         .addLineItem(SessionCreateParams.LineItem.builder()
            //                 .setPrice(priceId)
            //                 .setQuantity(1L)
            //                 .build())
            //         .setSuccessUrl(successUrl)
            //         .setCancelUrl(cancelUrl)
            //         .build();
            // return Session.create(params).getUrl();
            log.warn("[StripeService] Stripe not configured — returning mock checkout URL");
            return successUrl + "?mock=true";
        }
        // TODO: STRIPE — real implementation goes here
        return successUrl + "?mock=true";
    }

    /**
     * Creates a Stripe Billing Portal session so the customer can manage their subscription.
     *
     * @return Portal session URL to redirect the user to
     */
    public String createPortalSession(String customerId, String returnUrl) {
        if (!isConfigured()) {
            // TODO: STRIPE — Stripe.apiKey = stripeSecretKey;
            // com.stripe.param.billingportal.SessionCreateParams params =
            //         com.stripe.param.billingportal.SessionCreateParams.builder()
            //                 .setCustomer(customerId)
            //                 .setReturnUrl(returnUrl)
            //                 .build();
            // return com.stripe.model.billingportal.Session.create(params).getUrl();
            log.warn("[StripeService] Stripe not configured — returning mock portal URL");
            return returnUrl;
        }
        // TODO: STRIPE — real implementation goes here
        return returnUrl;
    }

    /**
     * Validates a Stripe webhook signature and returns the event type + data.
     * Called from the webhook endpoint.
     */
    public StripeWebhookEvent parseWebhookEvent(String payload, String sigHeader) {
        if (!isConfigured() || webhookSecret.isBlank()) {
            // TODO: STRIPE — Webhook.constructEvent(payload, sigHeader, webhookSecret)
            log.warn("[StripeService] Stripe not configured — skipping webhook validation");
            return StripeWebhookEvent.UNKNOWN;
        }
        // TODO: STRIPE — real implementation goes here
        return StripeWebhookEvent.UNKNOWN;
    }

    /**
     * Cancels a Stripe subscription immediately.
     */
    public void cancelSubscription(String stripeSubscriptionId) {
        if (!isConfigured()) {
            // TODO: STRIPE — Stripe.apiKey = stripeSecretKey;
            // Subscription sub = Subscription.retrieve(stripeSubscriptionId);
            // sub.cancel();
            log.warn("[StripeService] Stripe not configured — skipping subscription cancellation");
            return;
        }
        // TODO: STRIPE — real implementation goes here
    }

    // ── helper types ─────────────────────────────────────────────────────────

    public enum BillingPeriodParam { MONTHLY, ANNUAL }

    public enum StripeWebhookEvent {
        CHECKOUT_SESSION_COMPLETED,
        SUBSCRIPTION_UPDATED,
        SUBSCRIPTION_DELETED,
        INVOICE_PAYMENT_FAILED,
        UNKNOWN
    }
}
