package com.qalytix.repository;

import com.qalytix.entity.NotificationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    List<NotificationEvent> findAllByOrgIdOrderBySentAtDesc(Long orgId, Pageable pageable);
}
