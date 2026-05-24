package com.qalytix.billing;

import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanGuard {

    private final OrganizationRepository    orgRepository;
    private final JenkinsConfigRepository   jenkinsConfigRepository;
    private final OrganizationMemberRepository memberRepository;

    public void checkJenkinsConnectionLimit(Long orgId) {
        Plan plan = orgRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("Organization not found"))
                .getPlan();

        long current = jenkinsConfigRepository.countByOrgIdAndActiveTrue(orgId);
        int limit = jenkinsLimit(plan);

        if (limit != -1 && current >= limit) {
            throw new BadRequestException(
                    "Your " + plan.name() + " plan allows a maximum of " + limit +
                    " Jenkins connection(s). Upgrade to add more.");
        }
    }

    public void checkMemberLimit(Long orgId) {
        Plan plan = orgRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("Organization not found"))
                .getPlan();

        long current = memberRepository.countByOrganizationId(orgId);
        int limit = memberLimit(plan);

        if (limit != -1 && current >= limit) {
            throw new BadRequestException(
                    "Your " + plan.name() + " plan allows a maximum of " + limit +
                    " member(s). Upgrade to add more.");
        }
    }

    private int jenkinsLimit(Plan plan) {
        return switch (plan) {
            case FREE       -> 1;
            case PRO        -> 5;
            case ENTERPRISE -> -1; // unlimited
        };
    }

    private int memberLimit(Plan plan) {
        return switch (plan) {
            case FREE       -> 3;
            case PRO        -> 15;
            case ENTERPRISE -> -1;
        };
    }
}
