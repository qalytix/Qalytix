package com.qalytix.service.impl;

import com.qalytix.dto.request.CreateCheckoutRequest;
import com.qalytix.dto.response.BillingResponse;
import com.qalytix.dto.response.CheckoutResponse;
import com.qalytix.entity.Subscription;
import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.integration.stripe.StripeService;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.repository.OrganizationRepository;
import com.qalytix.repository.SubscriptionRepository;
import com.qalytix.security.TenantContext;
import com.qalytix.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final SubscriptionRepository        subscriptionRepository;
    private final OrganizationRepository        orgRepository;
    private final OrganizationMemberRepository  memberRepository;
    private final JenkinsConfigRepository       jenkinsConfigRepository;
    private final StripeService                 stripeService;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    // ── public API ───────────────────────────────────────────────────────────

    @Override
    public BillingResponse getCurrentBilling() {
        Long orgId = TenantContext.getOrgId();
        Subscription sub = getOrCreateSubscription(orgId);

        long jenkinsUsed = jenkinsConfigRepository.countByOrgIdAndActiveTrue(orgId);
        long membersUsed = memberRepository.countByOrganizationId(orgId);

        return new BillingResponse(
                sub.getPlan(),
                sub.getStatus(),
                sub.getBillingPeriod(),
                sub.getCurrentPeriodEnd(),
                sub.getTrialEndsAt(),
                stripeService.isConfigured(),
                jenkinsUsed,  jenkinsLimit(sub.getPlan()),
                membersUsed,  memberLimit(sub.getPlan()),
                retentionDays(sub.getPlan())
        );
    }

    @Override
    @Transactional
    public CheckoutResponse createCheckoutSession(CreateCheckoutRequest request) {
        Long orgId = TenantContext.getOrgId();

        if (request.plan() == Plan.FREE) {
            throw new BadRequestException("Cannot create a checkout session for the Free plan.");
        }

        Subscription sub = getOrCreateSubscription(orgId);
        String customerId = ensureStripeCustomer(orgId, sub);

        String checkoutUrl = stripeService.createCheckoutSession(
                customerId,
                request.plan(),
                request.billingPeriod(),
                appBaseUrl + "/billing/success",
                appBaseUrl + "/billing/cancel"
        );

        return new CheckoutResponse(checkoutUrl);
    }

    @Override
    @Transactional
    public CheckoutResponse createPortalSession() {
        Long orgId = TenantContext.getOrgId();
        Subscription sub = getOrCreateSubscription(orgId);
        String customerId = ensureStripeCustomer(orgId, sub);

        String portalUrl = stripeService.createPortalSession(
                customerId,
                appBaseUrl + "/billing"
        );

        return new CheckoutResponse(portalUrl);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Subscription getOrCreateSubscription(Long orgId) {
        return subscriptionRepository.findByOrgId(orgId)
                .orElseGet(() -> {
                    Subscription fresh = Subscription.builder()
                            .orgId(orgId)
                            .build();
                    return subscriptionRepository.save(fresh);
                });
    }

    private String ensureStripeCustomer(Long orgId, Subscription sub) {
        if (sub.getStripeCustomerId() != null && !sub.getStripeCustomerId().isBlank()) {
            return sub.getStripeCustomerId();
        }
        String org = orgRepository.findById(orgId)
                .map(o -> o.getName())
                .orElse("Unknown");

        String customerId = stripeService.createOrGetCustomer(orgId, org, "");
        sub.setStripeCustomerId(customerId);
        subscriptionRepository.save(sub);
        return customerId;
    }

    // ── plan limits (mirrors PlanGuard) ──────────────────────────────────────

    private int jenkinsLimit(Plan plan) {
        return switch (plan) {
            case FREE       -> 1;
            case PRO        -> 5;
            case ENTERPRISE -> -1;
        };
    }

    private int memberLimit(Plan plan) {
        return switch (plan) {
            case FREE       -> 3;
            case PRO        -> 15;
            case ENTERPRISE -> -1;
        };
    }

    private int retentionDays(Plan plan) {
        return switch (plan) {
            case FREE       -> 7;
            case PRO        -> 90;
            case ENTERPRISE -> 365;
        };
    }
}
