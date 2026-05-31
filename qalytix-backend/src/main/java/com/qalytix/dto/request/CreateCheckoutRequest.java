package com.qalytix.dto.request;

import com.qalytix.entity.enums.Plan;
import com.qalytix.integration.stripe.StripeService.BillingPeriodParam;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutRequest(
        @NotNull Plan plan,
        @NotNull BillingPeriodParam billingPeriod
) {}
