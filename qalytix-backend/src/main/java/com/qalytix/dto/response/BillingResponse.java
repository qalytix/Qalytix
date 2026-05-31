package com.qalytix.dto.response;

import com.qalytix.entity.enums.BillingPeriod;
import com.qalytix.entity.enums.Plan;
import com.qalytix.entity.enums.SubscriptionStatus;

import java.time.OffsetDateTime;

public record BillingResponse(
        Plan plan,
        SubscriptionStatus status,
        BillingPeriod billingPeriod,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime trialEndsAt,
        boolean stripeConfigured,

        // Usage counters
        long jenkinsConnectionsUsed,
        int  jenkinsConnectionsLimit,   // -1 = unlimited
        long membersUsed,
        int  membersLimit,              // -1 = unlimited
        int  dataRetentionDays
) {}
