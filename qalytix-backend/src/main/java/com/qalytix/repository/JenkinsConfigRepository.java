package com.qalytix.repository;

import com.qalytix.entity.JenkinsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JenkinsConfigRepository extends JpaRepository<JenkinsConfig, Long> {

    List<JenkinsConfig> findAllByOrgId(Long orgId);

    Optional<JenkinsConfig> findByIdAndOrgId(Long id, Long orgId);

    long countByOrgIdAndActiveTrue(Long orgId);

    boolean existsByOrgIdAndName(Long orgId, String name);

    List<JenkinsConfig> findAllByActiveTrue();
}
