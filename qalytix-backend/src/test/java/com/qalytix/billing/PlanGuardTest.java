package com.qalytix.billing;

import com.qalytix.entity.Subscription;
import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanGuardTest {

    @Mock SubscriptionRepository        subscriptionRepository;
    @Mock JenkinsConfigRepository       jenkinsConfigRepository;
    @Mock OrganizationMemberRepository  memberRepository;

    @InjectMocks PlanGuard planGuard;

    private Subscription sub(Plan plan) {
        return Subscription.builder().orgId(1L).plan(plan).build();
    }

    @Test
    void jenkins_freeAtLimit_throws() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.of(sub(Plan.FREE)));
        when(jenkinsConfigRepository.countByOrgIdAndActiveTrue(1L)).thenReturn(1L);

        assertThatThrownBy(() -> planGuard.checkJenkinsConnectionLimit(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("FREE");
    }

    @Test
    void jenkins_freeBelowLimit_passes() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.of(sub(Plan.FREE)));
        when(jenkinsConfigRepository.countByOrgIdAndActiveTrue(1L)).thenReturn(0L);

        assertThatCode(() -> planGuard.checkJenkinsConnectionLimit(1L)).doesNotThrowAnyException();
    }

    @Test
    void jenkins_enterpriseUnlimited_passes() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.of(sub(Plan.ENTERPRISE)));
        when(jenkinsConfigRepository.countByOrgIdAndActiveTrue(1L)).thenReturn(999L);

        assertThatCode(() -> planGuard.checkJenkinsConnectionLimit(1L)).doesNotThrowAnyException();
    }

    @Test
    void members_proAtLimit_throws() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.of(sub(Plan.PRO)));
        when(memberRepository.countByOrganizationId(1L)).thenReturn(15L);

        assertThatThrownBy(() -> planGuard.checkMemberLimit(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PRO");
    }

    @Test
    void members_proBelowLimit_passes() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.of(sub(Plan.PRO)));
        when(memberRepository.countByOrganizationId(1L)).thenReturn(14L);

        assertThatCode(() -> planGuard.checkMemberLimit(1L)).doesNotThrowAnyException();
    }

    @Test
    void noSubscriptionRow_defaultsToFree() {
        when(subscriptionRepository.findByOrgId(1L)).thenReturn(Optional.empty());
        when(jenkinsConfigRepository.countByOrgIdAndActiveTrue(1L)).thenReturn(1L);

        // FREE limit is 1, so at-limit should throw
        assertThatThrownBy(() -> planGuard.checkJenkinsConnectionLimit(1L))
                .isInstanceOf(BadRequestException.class);
    }
}
