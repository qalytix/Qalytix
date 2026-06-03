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

    long countByOrgId(Long orgId);

    long countByStartedAtAfter(Instant since);

    long countByOrgIdAndStatus(Long orgId, BuildStatus status);

    long countByOrgIdAndStartedAtAfter(Long orgId, Instant since);

    long countByOrgIdAndStatusAndStartedAtAfter(Long orgId, BuildStatus status, Instant since);

    @Query("""
            SELECT b FROM Build b
            WHERE b.orgId = :orgId
            ORDER BY b.startedAt DESC
            """)
    List<Build> findRecentByOrgId(@Param("orgId") Long orgId, Pageable pageable);

    // ── test-jobs-only variants ───────────────────────────────────────────────

    @Query("""
            SELECT COUNT(b) FROM Build b
            WHERE b.orgId = :orgId AND b.status = :status
              AND b.jobId IN (SELECT j.id FROM Job j WHERE j.orgId = :orgId AND j.isTestJob = true)
            """)
    long countByOrgIdAndStatusAndTestJobs(
            @Param("orgId") Long orgId, @Param("status") BuildStatus status);

    @Query("""
            SELECT COUNT(b) FROM Build b
            WHERE b.orgId = :orgId AND b.startedAt > :since
              AND b.jobId IN (SELECT j.id FROM Job j WHERE j.orgId = :orgId AND j.isTestJob = true)
            """)
    long countByOrgIdAndStartedAtAfterAndTestJobs(
            @Param("orgId") Long orgId, @Param("since") Instant since);

    @Query("""
            SELECT COUNT(b) FROM Build b
            WHERE b.orgId = :orgId AND b.status = :status AND b.startedAt > :since
              AND b.jobId IN (SELECT j.id FROM Job j WHERE j.orgId = :orgId AND j.isTestJob = true)
            """)
    long countByOrgIdAndStatusAndStartedAtAfterAndTestJobs(
            @Param("orgId") Long orgId, @Param("status") BuildStatus status, @Param("since") Instant since);

    @Query("""
            SELECT b FROM Build b
            WHERE b.orgId = :orgId
              AND b.jobId IN (SELECT j.id FROM Job j WHERE j.orgId = :orgId AND j.isTestJob = true)
            ORDER BY b.startedAt DESC
            """)
    List<Build> findRecentByOrgIdTestJobsOnly(@Param("orgId") Long orgId, Pageable pageable);

    /** Last N builds for a specific job, newest first — used to detect consecutive failures. */
    @Query("""
            SELECT b FROM Build b
            WHERE b.jobId = :jobId AND b.orgId = :orgId
            ORDER BY b.buildNumber DESC
            """)
    List<Build> findRecentByJobId(@Param("jobId") Long jobId, @Param("orgId") Long orgId, Pageable pageable);
}
