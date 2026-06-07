package com.qalytix.repository;

import com.qalytix.entity.Build;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.repository.projection.JobDailyStatusProjection;
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

    /**
     * Status of each job's latest build since the given instant — when a job ran more than
     * once in the period, only its most recent execution counts towards the totals.
     */
    @Query(value = """
            SELECT DISTINCT ON (b.job_id) b.status
            FROM builds b
            JOIN jobs j ON j.id = b.job_id
            WHERE b.org_id = :orgId
              AND b.started_at >= :since
              AND (:testJobsOnly = false OR j.is_test_job = true)
            ORDER BY b.job_id, b.started_at DESC
            """, nativeQuery = true)
    List<String> findLatestBuildStatusPerJobSince(
            @Param("orgId") Long orgId,
            @Param("since") Instant since,
            @Param("testJobsOnly") boolean testJobsOnly);

    /**
     * Per job, per UTC calendar day status history since the given instant — when a job ran
     * more than once in a day, only its latest execution that day is reported; days with no
     * build come back with a null status.
     */
    @Query(value = """
            WITH day_list AS (
                SELECT generate_series(
                           (:since AT TIME ZONE 'UTC')::date,
                           CURRENT_DATE,
                           INTERVAL '1 day'
                       )::date AS day
            ),
            latest_daily_builds AS (
                SELECT DISTINCT ON (b.job_id, (b.started_at AT TIME ZONE 'UTC')::date)
                       b.job_id AS job_id,
                       (b.started_at AT TIME ZONE 'UTC')::date AS build_date,
                       b.status AS status
                FROM builds b
                WHERE b.org_id = :orgId
                  AND b.started_at >= :since
                ORDER BY b.job_id, (b.started_at AT TIME ZONE 'UTC')::date, b.started_at DESC
            )
            SELECT j.id AS jobId,
                   COALESCE(j.display_name, j.jenkins_job_name) AS jobName,
                   TO_CHAR(dl.day, 'YYYY-MM-DD') AS day,
                   ldb.status AS status
            FROM jobs j
            CROSS JOIN day_list dl
            LEFT JOIN latest_daily_builds ldb ON ldb.job_id = j.id AND ldb.build_date = dl.day
            WHERE j.org_id = :orgId
              AND (:testJobsOnly = false OR j.is_test_job = true)
              AND EXISTS (SELECT 1 FROM builds b2 WHERE b2.job_id = j.id AND b2.org_id = :orgId)
            ORDER BY jobName, dl.day
            """, nativeQuery = true)
    List<JobDailyStatusProjection> findDailyStatusHistory(
            @Param("orgId") Long orgId,
            @Param("since") Instant since,
            @Param("testJobsOnly") boolean testJobsOnly);

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
