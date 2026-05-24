package com.qalytix.repository;

import com.qalytix.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByOrgId(Long orgId);

    Optional<Job> findByIdAndOrgId(Long id, Long orgId);

    Optional<Job> findByJenkinsConfigIdAndJenkinsJobName(Long jenkinsConfigId, String jenkinsJobName);
}
