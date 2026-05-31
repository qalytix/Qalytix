package com.qalytix.repository;

import com.qalytix.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {

    List<NotificationConfig> findAllByOrgIdOrderByCreatedAtDesc(Long orgId);

    List<NotificationConfig> findAllByOrgIdAndEnabledTrue(Long orgId);
}
