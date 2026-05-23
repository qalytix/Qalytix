package com.qalytix.service;

import com.qalytix.dto.request.UpdateMemberRoleRequest;
import com.qalytix.dto.response.MemberResponse;
import com.qalytix.entity.Organization;
import com.qalytix.entity.OrganizationMember;
import com.qalytix.entity.User;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;
import com.qalytix.entity.enums.OrgStatus;
import com.qalytix.entity.enums.Plan;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.security.TenantContext;
import com.qalytix.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock OrganizationMemberRepository memberRepository;

    @InjectMocks MemberServiceImpl memberService;

    // ------------------------------------------------------------------ fixtures

    private final Organization org = Organization.builder()
            .id(10L).name("Acme").slug("acme").plan(Plan.FREE).status(OrgStatus.ACTIVE).build();

    private final User ownerUser  = User.builder().id(1L).email("owner@example.com").fullName("Owner").build();
    private final User adminUser  = User.builder().id(2L).email("admin@example.com").fullName("Admin").build();
    private final User memberUser = User.builder().id(3L).email("member@example.com").fullName("Member").build();

    private final OrganizationMember ownerMember = OrganizationMember.builder()
            .id(1L).organization(org).user(ownerUser).role(MemberRole.OWNER).status(MemberStatus.ACTIVE).build();

    private final OrganizationMember adminMember = OrganizationMember.builder()
            .id(2L).organization(org).user(adminUser).role(MemberRole.ADMIN).status(MemberStatus.ACTIVE).build();

    private final OrganizationMember regularMember = OrganizationMember.builder()
            .id(3L).organization(org).user(memberUser).role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).build();

    private final AuthenticatedUser ownerCaller  = new AuthenticatedUser(1L, 10L, "owner@example.com", MemberRole.OWNER);
    private final AuthenticatedUser adminCaller  = new AuthenticatedUser(2L, 10L, "admin@example.com", MemberRole.ADMIN);

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(10L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------ listMembers

    @Test
    void listMembers_returnsActiveMembers() {
        when(memberRepository.findByOrganizationIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, adminMember));

        List<MemberResponse> result = memberService.listMembers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemberResponse::email)
                .containsExactly("owner@example.com", "admin@example.com");
    }

    // ------------------------------------------------------------------ updateRole

    @Test
    void updateRole_adminPromotesMemberToAdmin_succeeds() {
        when(memberRepository.findById(3L)).thenReturn(Optional.of(regularMember));
        when(memberRepository.save(any())).thenReturn(regularMember);

        MemberResponse result = memberService.updateRole(3L,
                new UpdateMemberRoleRequest(MemberRole.ADMIN), adminCaller);

        assertThat(result).isNotNull();
        verify(memberRepository).save(regularMember);
    }

    @Test
    void updateRole_adminTriesToPromoteToOwner_throwsBadRequest() {
        when(memberRepository.findById(3L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> memberService.updateRole(3L,
                new UpdateMemberRoleRequest(MemberRole.OWNER), adminCaller))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only an Owner");
    }

    @Test
    void updateRole_adminTriesToDemoteOwner_throwsBadRequest() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> memberService.updateRole(1L,
                new UpdateMemberRoleRequest(MemberRole.ADMIN), adminCaller))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only an Owner");
    }

    @Test
    void updateRole_demoteLastOwner_throwsBadRequest() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(ownerMember));
        // Only one owner in the org
        when(memberRepository.findByOrganizationIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember));

        assertThatThrownBy(() -> memberService.updateRole(1L,
                new UpdateMemberRoleRequest(MemberRole.ADMIN), ownerCaller))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one Owner");
    }

    @Test
    void updateRole_ownerDemotesSelfWhenAnotherOwnerExists_succeeds() {
        User secondOwnerUser = User.builder().id(5L).email("owner2@example.com").fullName("Owner2").build();
        OrganizationMember secondOwner = OrganizationMember.builder()
                .id(5L).organization(org).user(secondOwnerUser).role(MemberRole.OWNER).status(MemberStatus.ACTIVE).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(ownerMember));
        when(memberRepository.findByOrganizationIdAndStatus(10L, MemberStatus.ACTIVE))
                .thenReturn(List.of(ownerMember, secondOwner));
        when(memberRepository.save(any())).thenReturn(ownerMember);

        memberService.updateRole(1L, new UpdateMemberRoleRequest(MemberRole.ADMIN), ownerCaller);

        verify(memberRepository).save(ownerMember);
    }

    @Test
    void updateRole_memberNotInCurrentOrg_throwsNotFound() {
        Organization otherOrg = Organization.builder().id(99L).build();
        OrganizationMember foreignMember = OrganizationMember.builder()
                .id(9L).organization(otherOrg).user(memberUser).role(MemberRole.MEMBER).build();

        when(memberRepository.findById(9L)).thenReturn(Optional.of(foreignMember));

        assertThatThrownBy(() -> memberService.updateRole(9L,
                new UpdateMemberRoleRequest(MemberRole.ADMIN), adminCaller))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ removeMember

    @Test
    void removeMember_adminRemovesMember_succeeds() {
        when(memberRepository.findById(3L)).thenReturn(Optional.of(regularMember));

        memberService.removeMember(3L, adminCaller);

        verify(memberRepository).delete(regularMember);
    }

    @Test
    void removeMember_selfRemoval_throwsBadRequest() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> memberService.removeMember(2L, adminCaller))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot remove yourself");
    }

    @Test
    void removeMember_adminTriesToRemoveOwner_throwsBadRequest() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> memberService.removeMember(1L, adminCaller))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only an Owner");
    }

    @Test
    void removeMember_ownerRemovesAdmin_succeeds() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(adminMember));

        memberService.removeMember(2L, ownerCaller);

        verify(memberRepository).delete(adminMember);
    }
}
