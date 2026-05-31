package com.qalytix.entity;

import com.qalytix.entity.enums.NotificationChannel;
import com.qalytix.entity.enums.TriggerEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "config_id")
    private Long configId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 40)
    private TriggerEvent triggerEvent;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "build_number")
    private Integer buildNumber;

    @Column(name = "payload_summary")
    private String payloadSummary;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private OffsetDateTime sentAt = OffsetDateTime.now();
}
