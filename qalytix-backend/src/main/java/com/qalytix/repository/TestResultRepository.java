package com.qalytix.repository;

import com.qalytix.entity.TestResult;
import com.qalytix.repository.projection.FailureTrendProjection;
import com.qalytix.repository.projection.JobStatProjection;
import com.qalytix.repository.projection.ModuleStabilityProjection;
import com.qalytix.repository.projection.TestAggProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    boolean existsByBuildId(Long buildId);

    @Modifying
    @Query("DELETE FROM TestResult t WHERE t.buildId = :buildId")
    void deleteByBuildId(@Param("buildId") Long buildId);

    @Query(value = """
            SELECT TO_CHAR(b.started_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS date,
                   COUNT(tr.id)               AS total,
                   SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END)  AS failed,
                   SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END)  AS passed
            FROM test_results tr
            JOIN builds b ON tr.build_id = b.id
            WHERE tr.org_id = :orgId
              AND (:jobId IS NULL OR tr.job_id = :jobId)
              AND b.started_at >= :since
            GROUP BY TO_CHAR(b.started_at AT TIME ZONE 'UTC', 'YYYY-MM-DD')
            ORDER BY date
            """, nativeQuery = true)
    List<FailureTrendProjection> findFailureTrend(
            @Param("orgId") Long orgId,
            @Param("jobId") Long jobId,
            @Param("since") Instant since);

    @Query(value = """
            SELECT tr.test_suite AS testSuite,
                   tr.test_name  AS testName,
                   COUNT(*)      AS failCount
            FROM test_results tr
            WHERE tr.org_id = :orgId
              AND tr.status  = 'FAILED'
              AND (:jobId IS NULL OR tr.job_id = :jobId)
              AND tr.created_at >= :since
            GROUP BY tr.test_suite, tr.test_name
            ORDER BY failCount DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<TestAggProjection> findTopFailing(
            @Param("orgId") Long orgId,
            @Param("jobId") Long jobId,
            @Param("since") Instant since,
            @Param("lim") int limit);

    @Query(value = """
            SELECT tr.test_suite AS testSuite,
                   tr.test_name  AS testName,
                   COUNT(*)      AS totalRuns,
                   SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END) AS failCount,
                   SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END) AS passCount
            FROM test_results tr
            WHERE tr.org_id = :orgId
              AND (:jobId IS NULL OR tr.job_id = :jobId)
              AND tr.created_at >= :since
            GROUP BY tr.test_suite, tr.test_name
            HAVING SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END) > 0
               AND SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END) > 0
            ORDER BY (LEAST(SUM(CASE WHEN tr.status='FAILED' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN tr.status='PASSED' THEN 1 ELSE 0 END))::float / COUNT(*)) DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<TestAggProjection> findFlakyTests(
            @Param("orgId") Long orgId,
            @Param("jobId") Long jobId,
            @Param("since") Instant since,
            @Param("lim") int limit);

    @Query(value = """
            SELECT tr.test_suite AS moduleName,
                   COUNT(*)      AS total,
                   SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END) AS passed
            FROM test_results tr
            WHERE tr.org_id = :orgId
              AND (:jobId IS NULL OR tr.job_id = :jobId)
              AND tr.created_at >= :since
            GROUP BY tr.test_suite
            ORDER BY (SUM(CASE WHEN tr.status='PASSED' THEN 1 ELSE 0 END)::float / NULLIF(COUNT(*), 0)) ASC
            """, nativeQuery = true)
    List<ModuleStabilityProjection> findModuleStability(
            @Param("orgId") Long orgId,
            @Param("jobId") Long jobId,
            @Param("since") Instant since);

    @Query(value = """
            SELECT COALESCE(j.display_name, j.jenkins_job_name) AS jobName,
                   COUNT(tr.id) AS totalTests,
                   j.last_build_status AS latestBuildStatus,
                   SUM(CASE WHEN (b.started_at AT TIME ZONE 'UTC')::date = CURRENT_DATE - INTERVAL '1 day'
                            AND tr.status = 'PASSED' THEN 1 ELSE 0 END) AS yesterdayPassed,
                   SUM(CASE WHEN (b.started_at AT TIME ZONE 'UTC')::date = CURRENT_DATE
                            AND tr.status = 'PASSED' THEN 1 ELSE 0 END) AS todayPassed,
                   SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END) AS passedTotal,
                   SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END) AS failedTotal
            FROM jobs j
            JOIN builds b ON b.job_id = j.id AND b.org_id = :orgId
            JOIN test_results tr ON tr.build_id = b.id AND tr.org_id = :orgId
            WHERE j.org_id = :orgId
              AND b.started_at >= :since
            GROUP BY j.id, j.display_name, j.jenkins_job_name, j.last_build_status
            ORDER BY jobName
            """, nativeQuery = true)
    List<JobStatProjection> findJobStats(
            @Param("orgId") Long orgId,
            @Param("since") Instant since);
}
