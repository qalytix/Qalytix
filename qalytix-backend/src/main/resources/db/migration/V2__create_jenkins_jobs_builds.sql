-- Jenkins server configurations (one per org per connection, plan-gated)
CREATE TABLE jenkins_configs (
    id            BIGSERIAL PRIMARY KEY,
    org_id        BIGINT NOT NULL REFERENCES organizations(id),
    name          VARCHAR(255) NOT NULL,
    url           VARCHAR(512) NOT NULL,
    username      VARCHAR(255) NOT NULL,
    api_token     TEXT         NOT NULL,
    poll_interval VARCHAR(100) NOT NULL DEFAULT '*/5 * * * *',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_synced_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jenkins_configs_org_id ON jenkins_configs(org_id);
CREATE UNIQUE INDEX idx_jenkins_configs_org_name ON jenkins_configs(org_id, name);

-- Jenkins jobs discovered from the configured server
CREATE TABLE jobs (
    id                BIGSERIAL PRIMARY KEY,
    org_id            BIGINT NOT NULL REFERENCES organizations(id),
    jenkins_config_id BIGINT NOT NULL REFERENCES jenkins_configs(id),
    jenkins_job_name  VARCHAR(512) NOT NULL,
    display_name      VARCHAR(512),
    url               VARCHAR(512),
    last_build_number INT,
    last_build_status VARCHAR(50),
    last_build_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_org_id               ON jobs(org_id);
CREATE INDEX idx_jobs_jenkins_config_id    ON jobs(jenkins_config_id);
CREATE UNIQUE INDEX idx_jobs_config_name   ON jobs(jenkins_config_id, jenkins_job_name);

-- Individual build runs for a job
CREATE TABLE builds (
    id                BIGSERIAL PRIMARY KEY,
    org_id            BIGINT NOT NULL REFERENCES organizations(id),
    job_id            BIGINT NOT NULL REFERENCES jobs(id),
    build_number      INT    NOT NULL,
    status            VARCHAR(50) NOT NULL,   -- SUCCESS | FAILURE | UNSTABLE | ABORTED | IN_PROGRESS
    duration_ms       BIGINT,
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ,
    url               VARCHAR(512),
    triggered_by      VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_builds_org_id  ON builds(org_id);
CREATE INDEX idx_builds_job_id  ON builds(job_id);
CREATE UNIQUE INDEX idx_builds_job_number ON builds(job_id, build_number);
