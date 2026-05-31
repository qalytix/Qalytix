package com.qalytix.entity;

import com.qalytix.entity.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    @Column(name = "on_build_failure", nullable = false)
    @Builder.Default
    private boolean onBuildFailure = true;

    @Column(name = "on_consecutive_failures", nullable = false)
    @Builder.Default
    private boolean onConsecutiveFailures = false;

    @Column(name = "consecutive_threshold", nullable = false)
    @Builder.Default
    private int consecutiveThreshold = 3;

    @Column(name = "on_flaky_threshold", nullable = false)
    @Builder.Default
    private boolean onFlakyThreshold = false;

    @Column(name = "flaky_score_threshold", nullable = false, columnDefinition = "numeric(5,3)")
    @Builder.Default
    private double flakyScoreThreshold = 0.5;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
