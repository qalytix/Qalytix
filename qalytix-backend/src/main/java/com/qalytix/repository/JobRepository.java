package com.qalytix.repository;

import com.qalytix.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByOrgId(Long orgId);

    /** Filter by org, optionally restricted to test jobs only. */
    List<Job> findAllByOrgIdAndIsTestJob(Long orgId, boolean isTestJob);

    /** Filter by org and a view name substring (pipe-delimited match). */
    @Query("SELECT j FROM Job j WHERE j.orgId = :orgId AND j.viewNames LIKE %:view%")
    List<Job> findAllByOrgIdAndView(@Param("orgId") Long orgId, @Param("view") String view);

    /** Filter by org, view, and test-job flag. */
    @Query("SELECT j FROM Job j WHERE j.orgId = :orgId AND j.viewNames LIKE %:view% AND j.isTestJob = :isTestJob")
    List<Job> findAllByOrgIdAndViewAndIsTestJob(
            @Param("orgId") Long orgId,
            @Param("view") String view,
            @Param("isTestJob") boolean isTestJob);

    Optional<Job> findByIdAndOrgId(Long id, Long orgId);

    Optional<Job> findByJenkinsConfigIdAndJenkinsJobName(Long jenkinsConfigId, String jenkinsJobName);

    /** Mark a job as a test job (called after first test result ingestion). */
    @Modifying
    @Query("UPDATE Job j SET j.isTestJob = true WHERE j.id = :id")
    void markAsTestJob(@Param("id") Long id);
}
