package com.qalytix.entity;

import com.qalytix.entity.enums.BuildStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "jenkins_config_id", nullable = false)
    private Long jenkinsConfigId;

    @Column(nullable = false)
    private String jenkinsJobName;

    private String displayName;
    private String url;
    private Integer lastBuildNumber;

    @Enumerated(EnumType.STRING)
    private BuildStatus lastBuildStatus;

    private Instant lastBuildAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
