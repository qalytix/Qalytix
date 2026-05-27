-- V4: add is_test_job flag and view_names to jobs
-- is_test_job is set to true automatically when the first test result is ingested for a job
-- view_names stores pipe-delimited Jenkins view names, e.g. |All|Backend|Nightly|

ALTER TABLE jobs
    ADD COLUMN is_test_job BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE jobs
    ADD COLUMN view_names TEXT NOT NULL DEFAULT '|All|';

-- Index to speed up analytics and dashboard queries that filter on is_test_job
CREATE INDEX idx_jobs_org_is_test_job ON jobs (org_id, is_test_job);
