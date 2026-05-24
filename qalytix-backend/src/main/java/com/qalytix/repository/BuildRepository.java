package com.qalytix.repository;

import com.qalytix.entity.Build;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildRepository extends JpaRepository<Build, Long> {

    Page<Build> findAllByJobIdAndOrgId(Long jobId, Long orgId, Pageable pageable);

    Optional<Build> findByIdAndOrgId(Long id, Long orgId);

    Optional<Build> findByJobIdAndBuildNumber(Long jobId, int buildNumber);
}
