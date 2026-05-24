package com.qalytix.repository;

import com.qalytix.entity.Build;
import com.qalytix.entity.enums.BuildStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BuildRepository extends JpaRepository<Build, Long> {

    Page<Build> findAllByJobIdAndOrgId(Long jobId, Long orgId, Pageable pageable);

    Optional<Build> findByIdAndOrgId(Long id, Long orgId);

    Optional<Build> findByJobIdAndBuildNumber(Long jobId, int buildNumber);

    long countByOrgIdAndStatus(Long orgId, BuildStatus status);

    long countByOrgIdAndStartedAtAfter(Long orgId, Instant since);

    long countByOrgIdAndStatusAndStartedAtAfter(Long orgId, BuildStatus status, Instant since);

    @Query("""
            SELECT b FROM Build b
            WHERE b.orgId = :orgId
            ORDER BY b.startedAt DESC
            """)
    List<Build> findRecentByOrgId(@Param("orgId") Long orgId, Pageable pageable);
}
