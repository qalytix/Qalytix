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

    /** True once at least one test result has been ingested for this job. */
    @Column(name = "is_test_job", nullable = false)
    private boolean isTestJob = false;

    /**
     * Pipe-delimited Jenkins view names this job belongs to, e.g. "|All|Backend|".
     * Always contains "|All|" (Jenkins' default view).
     */
    @Column(name = "view_names", nullable = false)
    private String viewNames = "|All|";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
