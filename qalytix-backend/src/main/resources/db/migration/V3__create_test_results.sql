-- Individual test case outcomes per build, ingested from JUnit XML artifacts
CREATE TABLE test_results (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT        NOT NULL REFERENCES organizations(id),
    build_id        BIGINT        NOT NULL REFERENCES builds(id),
    job_id          BIGINT        NOT NULL REFERENCES jobs(id),
    test_suite      VARCHAR(512)  NOT NULL,   -- classname / package
    test_name       VARCHAR(512)  NOT NULL,   -- method name
    status          VARCHAR(20)   NOT NULL,   -- PASSED | FAILED | SKIPPED | ERROR
    duration_ms     BIGINT,
    failure_message TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_test_results_org_id   ON test_results(org_id);
CREATE INDEX idx_test_results_build_id ON test_results(build_id);
CREATE INDEX idx_test_results_job_id   ON test_results(job_id);
CREATE INDEX idx_test_results_lookup   ON test_results(org_id, test_suite, test_name);
CREATE INDEX idx_test_results_created  ON test_results(org_id, created_at);
