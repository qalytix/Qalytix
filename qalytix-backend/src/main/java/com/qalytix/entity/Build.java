package com.qalytix.entity;

import com.qalytix.entity.enums.BuildStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "builds")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Integer buildNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildStatus status;

    private Long durationMs;
    private Instant startedAt;
    private Instant finishedAt;
    private String url;
    private String triggeredBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
