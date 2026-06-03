package com.qalytix.dto.response;

import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;
import com.qalytix.entity.enums.SubscriptionStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrgDetailResponse(
        Long           id,
        String         name,
        String         slug,
        Plan           plan,
        OrgStatus      status,
        OffsetDateTime createdAt,

        // Subscription
        SubscriptionStatus subscriptionStatus,
        String             stripeCustomerId,

        // Usage
        long memberCount,
        long jenkinsConfigCount,
        long buildCount,
        long testResultCount,

        List<MemberResponse>  members,
        List<JenkinsConfigSummary> jenkinsConfigs
) {
    public record JenkinsConfigSummary(Long id, String name, String url, boolean active) {}
}
