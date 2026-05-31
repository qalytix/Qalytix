package com.qalytix.billing;

import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanGuard {

    private final SubscriptionRepository        subscriptionRepository;
    private final JenkinsConfigRepository       jenkinsConfigRepository;
    private final OrganizationMemberRepository  memberRepository;

    // ── limit checks ─────────────────────────────────────────────────────────

    public void checkJenkinsConnectionLimit(Long orgId) {
        Plan plan = getPlan(orgId);
        long current = jenkinsConfigRepository.countByOrgIdAndActiveTrue(orgId);
        int  limit   = jenkinsLimit(plan);

        if (limit != -1 && current >= limit) {
            throw new BadRequestException(
                    "Your " + plan.name() + " plan allows a maximum of " + limit +
                    " Jenkins connection(s). Upgrade to add more.");
        }
    }

    public void checkMemberLimit(Long orgId) {
        Plan plan = getPlan(orgId);
        long current = memberRepository.countByOrganizationId(orgId);
        int  limit   = memberLimit(plan);

        if (limit != -1 && current >= limit) {
            throw new BadRequestException(
                    "Your " + plan.name() + " plan allows a maximum of " + limit +
                    " member(s). Upgrade to add more.");
        }
    }

    public int clampDataRetentionDays(Long orgId, int requestedDays) {
        return Math.min(requestedDays, retentionDays(getPlan(orgId)));
    }

    // ── plan resolution ──────────────────────────────────────────────────────

    /**
     * Resolves the plan from the subscriptions table.
     * Falls back to FREE if no subscription row exists yet (e.g. new org, migration lag).
     */
    public Plan getPlan(Long orgId) {
        return subscriptionRepository.findByOrgId(orgId)
                .map(s -> s.getPlan())
                .orElse(Plan.FREE);
    }

    // ── limit tables ─────────────────────────────────────────────────────────

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
