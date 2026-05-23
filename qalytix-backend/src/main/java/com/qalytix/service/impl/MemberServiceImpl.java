package com.qalytix.service.impl;

import com.qalytix.dto.request.UpdateMemberRoleRequest;
import com.qalytix.dto.response.MemberResponse;
import com.qalytix.entity.OrganizationMember;
import com.qalytix.entity.enums.MemberRole;
import com.qalytix.entity.enums.MemberStatus;
import com.qalytix.exception.BadRequestException;
import com.qalytix.exception.ResourceNotFoundException;
import com.qalytix.repository.OrganizationMemberRepository;
import com.qalytix.security.AuthenticatedUser;
import com.qalytix.security.TenantContext;
import com.qalytix.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final OrganizationMemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers() {
        return memberRepository
                .findByOrganizationIdAndStatus(TenantContext.getOrgId(), MemberStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse updateRole(Long memberId, UpdateMemberRoleRequest request, AuthenticatedUser caller) {
        OrganizationMember target = findMemberInCurrentOrg(memberId);

        // Only OWNER can promote/demote to or from OWNER
        if (target.getRole() == MemberRole.OWNER || request.role() == MemberRole.OWNER) {
            if (caller.role() != MemberRole.OWNER) {
                throw new BadRequestException("Only an Owner can assign or remove the Owner role");
            }
        }

        // Prevent the last OWNER from demoting themselves
        if (target.getRole() == MemberRole.OWNER && request.role() != MemberRole.OWNER) {
            long ownerCount = memberRepository
                    .findByOrganizationIdAndStatus(TenantContext.getOrgId(), MemberStatus.ACTIVE)
                    .stream()
                    .filter(m -> m.getRole() == MemberRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new BadRequestException("Organization must have at least one Owner");
            }
        }

        target.setRole(request.role());
        return toResponse(memberRepository.save(target));
    }

    @Override
    @Transactional
    public void removeMember(Long memberId, AuthenticatedUser caller) {
        OrganizationMember target = findMemberInCurrentOrg(memberId);

        if (target.getUser().getId().equals(caller.userId())) {
            throw new BadRequestException("You cannot remove yourself from the organization");
        }

        // Only OWNER can remove another OWNER
        if (target.getRole() == MemberRole.OWNER && caller.role() != MemberRole.OWNER) {
            throw new BadRequestException("Only an Owner can remove another Owner");
        }

        memberRepository.delete(target);
    }

    // ------------------------------------------------------------------ helpers

    private OrganizationMember findMemberInCurrentOrg(Long memberId) {
        OrganizationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!member.getOrganization().getId().equals(TenantContext.getOrgId())) {
            throw new ResourceNotFoundException("Member not found");
        }
        return member;
    }

    private MemberResponse toResponse(OrganizationMember m) {
        return new MemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getRole(),
                m.getStatus(),
                m.getJoinedAt()
        );
    }
}
