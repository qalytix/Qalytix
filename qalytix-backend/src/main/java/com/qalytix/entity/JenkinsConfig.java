package com.qalytix.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "jenkins_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JenkinsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String apiToken;

    @Column(nullable = false)
    private String pollInterval;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private Instant lastSyncedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
        if (pollInterval == null) pollInterval = "*/5 * * * *";
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
