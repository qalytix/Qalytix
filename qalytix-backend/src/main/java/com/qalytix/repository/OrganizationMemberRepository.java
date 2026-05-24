package com.qalytix.repository;

import com.qalytix.entity.OrganizationMember;
import com.qalytix.entity.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByUserIdAndStatus(Long userId, MemberStatus status);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long orgId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long orgId, Long userId);

    List<OrganizationMember> findByOrganizationIdAndStatus(Long orgId, MemberStatus status);

    long countByOrganizationId(Long orgId);
}
