-- Phase 6: Notifications
-- notification_configs: one row per webhook destination configured by an org.
-- notification_events:  audit log of every notification attempt.

CREATE TABLE notification_configs (
    id          BIGSERIAL       PRIMARY KEY,
    org_id      BIGINT          NOT NULL REFERENCES organizations(id),

    name        VARCHAR(100)    NOT NULL,          -- user-supplied label
    channel     VARCHAR(20)     NOT NULL,          -- TEAMS | SLACK
    webhook_url TEXT            NOT NULL,

    -- Trigger flags (each can be toggled independently)
    on_build_failure        BOOLEAN NOT NULL DEFAULT TRUE,
    on_consecutive_failures BOOLEAN NOT NULL DEFAULT FALSE,
    consecutive_threshold   INT     NOT NULL DEFAULT 3,   -- fires after N straight failures
    on_flaky_threshold      BOOLEAN NOT NULL DEFAULT FALSE,
    flaky_score_threshold   NUMERIC(5,3) NOT NULL DEFAULT 0.5,

    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_configs_org ON notification_configs(org_id);

CREATE TABLE notification_events (
    id              BIGSERIAL   PRIMARY KEY,
    org_id          BIGINT      NOT NULL REFERENCES organizations(id),
    config_id       BIGINT      REFERENCES notification_configs(id) ON DELETE SET NULL,

    channel         VARCHAR(20) NOT NULL,
    trigger_event   VARCHAR(40) NOT NULL,  -- BUILD_FAILURE | CONSECUTIVE_FAILURES | FLAKY_THRESHOLD
    job_name        VARCHAR(255),
    build_number    INT,
    payload_summary TEXT,                  -- short human-readable summary of what was sent

    success         BOOLEAN     NOT NULL,
    error_message   TEXT,

    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_events_org ON notification_events(org_id);
CREATE INDEX idx_notif_events_config ON notification_events(config_id);
