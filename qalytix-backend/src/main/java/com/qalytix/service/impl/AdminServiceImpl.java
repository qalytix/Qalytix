package com.qalytix.service.impl;

import com.qalytix.dto.request.AdminChangePlanRequest;
import com.qalytix.dto.response.AdminOrgDetailResponse;
import com.qalytix.dto.response.AdminOrgDetailResponse.JenkinsConfigSummary;
import com.qalytix.dto.response.AdminOrgResponse;
import com.qalytix.dto.response.AdminPlatformStatsResponse;
import com.qalytix.dto.response.MemberResponse;
import com.qalytix.entity.Organization;
import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;
import com.qalytix.entity.enums.SubscriptionStatus;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.*;
import com.qalytix.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final OrganizationRepository        orgRepository;
    private final OrganizationMemberRepository  memberRepository;
    private final JenkinsConfigRepository       jenkinsConfigRepository;
    private final BuildRepository               buildRepository;
    private final TestResultRepository          testResultRepository;
    private final SubscriptionRepository        subscriptionRepository;
    private final UserRepository                userRepository;

    @Override
    public AdminPlatformStatsResponse getPlatformStats() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long totalOrgs         = orgRepository.count();
        long activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPlan() != Plan.FREE)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.TRIALING)
                .count();
        long totalUsers        = userRepository.count();
        long totalBuilds       = buildRepository.count();
        long totalTestResults  = testResultRepository.count();
        long buildsLast24h     = buildRepository.countByStartedAtAfter(since24h);
        long testResultsLast24h = testResultRepository.countByCreatedAtAfter(since24h);

        return new AdminPlatformStatsResponse(
                totalOrgs, activeSubscriptions, totalUsers,
                totalBuilds, totalTestResults, buildsLast24h, testResultsLast24h
        );
    }

    @Override
    public List<AdminOrgResponse> listOrgs(String planFilter, String statusFilter) {
        return orgRepository.findAll().stream()
                .filter(o -> planFilter == null   || planFilter.isBlank()   || o.getPlan().name().equalsIgnoreCase(planFilter))
                .filter(o -> statusFilter == null || statusFilter.isBlank() || o.getStatus().name().equalsIgnoreCase(statusFilter))
                .map(this::toOrgResponse)
                .toList();
    }

    @Override
    public AdminOrgDetailResponse getOrgDetail(Long orgId) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));

        var subscription = subscriptionRepository.findByOrgId(orgId).orElse(null);

        List<MemberResponse> members = memberRepository.findAllByOrganizationId(orgId).stream()
                .map(m -> new MemberResponse(
                        m.getId(), m.getUser().getId(), m.getUser().getEmail(),
                        m.getUser().getFullName(), m.getRole(), m.getStatus(), m.getJoinedAt()))
                .toList();

        List<JenkinsConfigSummary> configs = jenkinsConfigRepository.findAllByOrgId(orgId).stream()
                .map(c -> new JenkinsConfigSummary(c.getId(), c.getName(), c.getUrl(), c.isActive()))
                .toList();

        long buildCount      = buildRepository.countByOrgId(orgId);
        long testResultCount = testResultRepository.countByOrgId(orgId);

        return new AdminOrgDetailResponse(
                org.getId(), org.getName(), org.getSlug(), org.getPlan(), org.getStatus(), org.getCreatedAt(),
                subscription != null ? subscription.getStatus() : null,
                subscription != null ? subscription.getStripeCustomerId() : null,
                members.size(), configs.size(), buildCount, testResultCount,
                members, configs
        );
    }

    @Override
    @Transactional
    public AdminOrgResponse changePlan(Long orgId, AdminChangePlanRequest request) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
        org.setPlan(request.plan());
        orgRepository.save(org);

        // Mirror change in subscriptions table
        subscriptionRepository.findByOrgId(orgId).ifPresent(sub -> {
            sub.setPlan(request.plan());
            subscriptionRepository.save(sub);
        });

        return toOrgResponse(org);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AdminOrgResponse toOrgResponse(Organization org) {
        long memberCount  = memberRepository.countByOrganizationId(org.getId());
        long jenkinsCount = jenkinsConfigRepository.countByOrgIdAndActiveTrue(org.getId());
        long buildCount   = buildRepository.countByOrgId(org.getId());
        return new AdminOrgResponse(
                org.getId(), org.getName(), org.getSlug(), org.getPlan(), org.getStatus(),
                memberCount, jenkinsCount, buildCount, org.getCreatedAt()
        );
    }
}
