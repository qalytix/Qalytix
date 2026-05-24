# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [0.4.1] - 2026-05-24

### Backend

**Added**
- `findJobStats` query now uses `LEFT JOIN` so jobs with completed builds but zero ingested test results appear in the Job Summary table (previously excluded by the inner join)
- `noResultBuilds` subquery per job: counts completed (non-`IN_PROGRESS`) builds in the window that have no associated `test_results` rows
- `JobStatProjection.getNoResultBuilds()` getter; `AnalyticsSummaryResponse.JobStat` includes `noResultBuilds` field
- `findJobStats` now filters to jobs with at least one build in the date window (via `EXISTS` subquery) to avoid showing stale, never-built jobs

### Frontend

**Added**
- Job Summary table: search input with clear button — filters rows by job name as you type
- Status filter pill group — All / Success / Failure / Unstable / Aborted (buttons generated from live data)
- "Warnings only" toggle — visible when any job has `noResultBuilds > 0`; narrows table to jobs needing attention
- Result counter ("3 of 4 jobs") and "Clear filters" link appear when any filter is active
- Warning indicator on jobs with `noResultBuilds > 0`: amber row background + `⚠` icon next to test count with tooltip explaining the fix; Pass % / Fail % show "—" when no test data exists at all

**Fixed**
- `AlertTriangle title` prop replaced with `<span title={...}>` wrapper to satisfy Lucide's TypeScript types

---

## [0.4.0] - 2026-05-24

Phase 4 — Test Analytics & Intelligence

### Backend

**Added**
- Flyway migration `V5__create_test_results.sql`: `test_results` table with `org_id`, `build_id`, `job_id`, `test_suite`, `test_name`, `status`, `duration_ms`, `failure_message`
- `TestResult` entity with full JPA mapping; `TestStatus` enum (PASSED / FAILED / SKIPPED / ERROR)
- `JUnitXmlParser`: parses `<testsuite>` / `<testcase>` elements from archived Jenkins artifacts; maps `failure`/`error` elements to `FAILED`, `skipped` to `SKIPPED`
- `CucumberJsonParser`: parses native Cucumber JSON report format; detects Cucumber JSON via `isCucumberJson()` checking for `elements` + `uri`/`keyword` fields; converts nanosecond durations to milliseconds; skips Background elements; truncates failure messages to 2 000 chars
- `TestResultIngestionServiceImpl`: three-tier fallback strategy — (1) Jenkins Test Report API `/testReport/api/json`, (2) archived JUnit XML artifacts filtered by surefire/test-result paths, (3) archived Cucumber JSON artifacts; idempotent via `existsByBuildId` check
- `JenkinsTestReportResponse`: record-based response model for Jenkins `/testReport/api/json` endpoint
- `AnalyticsService` / `AnalyticsServiceImpl`:
  - Failure rate trend by day (configurable `days` window)
  - Flaky test detection: tests that alternated pass/fail across builds, ranked by flakiness score
  - Top N failing tests across the retention window
  - Module (test suite) stability: pass rate, total runs, failure count per suite
  - Job summary stats: total tests, latest build status, yesterday vs today passed counts, trend direction, pass %, fail %
- `AnalyticsController` at `/api/v1/analytics/summary`: single endpoint returning all analytics data; date-range filter via `?days=N`; `PlanGuard` enforces retention window per plan tier
- `AnalyticsSummaryResponse` DTO: `failureTrend`, `topFailingTests`, `flakyTests`, `moduleStats`, `jobStats`; inner records `FailureTrend`, `TopFailingTest`, `FlakyTest`, `ModuleStat`, `JobStat`
- `JobStatProjection` JPA projection interface for native `findJobStats` query
- `TestResultRepository`: `existsByBuildId`, `findFailureTrend` (native query with `TO_CHAR` for string date projection), `findTopFailingTests`, `findFlakyTestCandidates`, `findModuleStats`, `findJobStats`

**Fixed**
- `JwtAuthFilter`: removed `SecurityContextHolder.clearContext()` from `finally` block — it was wiping authentication after `chain.doFilter()` returned under Spring Security 7's `SecurityContextHolderFilter`, causing all authenticated requests to receive 403
- `SecurityConfig`: added custom `authenticationEntryPoint` returning HTTP 401 (Spring Security 7 defaults to 403 for unauthenticated requests, breaking the Axios 401-refresh interceptor); added `/ws/**` to public endpoints so SockJS HTTP handshake probes pass through
- `dev.sh`: fixed all four `(( i++ ))` occurrences → `(( i++ )) || true` to prevent `set -euo pipefail` from exiting the script when the counter starts at zero
- `TestResultRepository.findFailureTrend`: changed `CAST(... AS DATE)` (returns `LocalDate`) to `TO_CHAR(... 'YYYY-MM-DD')` (returns `VARCHAR`) to match `String` projection getter and resolve 500 on analytics load
- `findJobStats` native query: replaced non-existent `j.name` column with `COALESCE(j.display_name, j.jenkins_job_name)`; reads `j.last_build_status` from cached jobs table

### Frontend

**Added**
- `AnalyticsPage`: full analytics dashboard with date-range selector and job filter
  - Failure trend line chart (Chart.js) — pass/fail counts by day
  - Flaky tests table: test name, suite, flakiness score, trend indicator
  - Top failing tests bar chart: test name, failure count, fail rate %
  - Module stability table: suite name, total runs, pass rate, failure count
  - Job Summary table: Job, No. of Tests, Build Status badge, Yesterday Passed, Today Passed, Trend badge, Pass %, Fail %
- `BuildBadge` component: coloured status chips for SUCCESS / FAILURE / UNSTABLE / ABORTED / IN_PROGRESS
- `TrendBadge` component: Up (green ↑) / Down (red ↓) / Stable (grey →) indicators
- `JobStat` TypeScript interface; `jobStats` field added to `AnalyticsSummaryResponse` type

**Fixed**
- `JenkinsPage` edit form: `autoComplete="new-password"` on API token field prevents browser autofill from replacing the field with stale credentials; added placeholder and helper text clarifying the field is optional on edit
- `JenkinsPage` error handling: `handleTest` and `handleSync` now extract `err.response?.data?.detail ?? err.response?.data?.message` for accurate backend error messages; toast timeout extended to 6 000 ms

---

## [0.3.0] - 2026-05-24

Phase 3 — Live Dashboard & Real-time Monitoring

### Backend

**Added**
- Spring WebSocket (STOMP over SockJS) at `/ws`; `StompAuthChannelInterceptor` validates JWT on CONNECT frame and sets `TenantContext` for the WebSocket session
- Org-scoped STOMP topics: `/topic/org/{orgId}/builds`
- `BuildEventPublisher`: broadcasts `BuildStatusEvent` (buildId, jobName, status, timestamp) when a build status changes during ingestion
- `DashboardController` at `/api/v1/dashboard/stats`: active build count, today's total / passed / failed / pass-rate, recent build activity feed (last 20 events)
- `DashboardStatsResponse` DTO with active builds, daily counters, and recent builds list

### Frontend

**Added**
- `useWebSocket` hook: connects via SockJS + STOMP with JWT; subscribes to org-scoped topic; exposes `connected` flag and latest event
- Live dashboard page: active/running builds widget (live-updating), today's pass/fail/total counters, recent build activity feed with animated status badges
- `BuildStatusBadge` component: RUNNING (pulsing blue), PASSED (green), FAILED (red), UNSTABLE (yellow), ABORTED (grey)

---

## [0.2.0] - 2026-05-24

Phase 2 — Jenkins Integration & Data Ingestion

### Backend

**Added**
- Flyway migrations: `V2__create_jenkins_configs.sql`, `V3__create_jobs.sql`, `V4__create_builds.sql`
- `JenkinsConfig` entity (org-scoped): Jenkins URL, username, API token, polling interval, active flag
- `Job` entity: Jenkins job name, display name, last build status, last build number, sync timestamps
- `Build` entity: build number, status (`BuildStatus` enum), started/finished timestamps, duration, URL
- `JenkinsClient`: `testConnection`, `fetchJobs`, `fetchBuildDetails`, `fetchArtifacts`, `downloadArtifact`, `fetchTestReport` via Jenkins REST API with HTTP Basic auth
- `JenkinsIngestionService` / `JenkinsIngestionServiceImpl`: `syncConfig` — upsert jobs and builds, publish `BuildStatusEvent` on status change
- `JenkinsPollingService`: `@Scheduled` task polling all active configs at configurable intervals
- `PlanGuard.check(orgId, Feature.JENKINS_CONNECTION)`: enforces per-plan Jenkins connection limits before config creation
- `JenkinsController` at `/api/v1/jenkins`: CRUD for Jenkins configs, `POST /test-connection`, `POST /{id}/sync`
- `JobController` at `/api/v1/jobs`: list jobs (org-scoped), job detail
- `BuildController` at `/api/v1/builds`: builds for a job, build detail

### Frontend

**Added**
- `JenkinsPage`: add/edit Jenkins server form with test-connection button; config list with sync trigger; delete with confirmation
- Jobs list page: all synced jobs with last build status badge and last sync time
- Build history page for a selected job: pass/fail timeline, duration, build number links

---

## [0.1.0] - 2026-05-24

Phase 1 — SaaS Foundation & Authentication

### Backend

**Added**
- Multi-tenant data model: `organizations`, `users`, `organization_members`, `invitations`, `refresh_tokens` tables via Flyway migration `V1__create_organizations_and_users.sql`
- Entities: `Organization`, `User`, `OrganizationMember`, `Invitation`, `RefreshToken` with full JPA mappings
- Enums: `Plan` (FREE / PRO / ENTERPRISE), `OrgStatus`, `MemberRole` (OWNER / ADMIN / MEMBER), `MemberStatus`
- JWT authentication: stateless; claims include `userId` / `orgId` / `email` / `role`; access tokens in-memory, refresh tokens stored as SHA-256 hash
- `JwtUtil` — token generation and validation (JJWT 0.12.x)
- `JwtAuthFilter` — `OncePerRequestFilter`; populates `SecurityContext` + `TenantContext`
- `TenantContext` — `ThreadLocal<Long>` for current org scoping; prevents cross-tenant data leaks
- `AuthenticatedUser` — custom `Principal` record injected via `@AuthenticationPrincipal`
- `SecurityConfig` — stateless `SecurityFilterChain`; public endpoints: `/api/v1/auth/**`, `/api/v1/invitations/accept`, `/actuator/health`, Swagger UI
- `AuthService` — register (creates User + Organization + OWNER membership atomically), login, token refresh, logout (revokes all refresh tokens for user + org)
- `OrgService` — fetch current org details
- `MemberService` — list members; update role (OWNER-only rules, last-OWNER guard); remove member (self-removal guard, cross-OWNER guard)
- `InvitationService` — send invite (dedup, 7-day expiry, email stub); accept invite (existing user join or new user registration)
- REST controllers: `AuthController`, `OrgController`, `MemberController`, `InvitationController`
- `GlobalExceptionHandler` — RFC 9457 `ProblemDetail` responses; field-level validation errors as `errors` map
- `ApiResponse<T>` — unified response envelope
- `AppProperties` — `@ConfigurationProperties(prefix="app")` for JWT secret and expiry config

### Frontend

**Added**
- React 19 + TypeScript + Vite scaffold
- Tailwind CSS v4 with `@theme` custom color tokens
- React Router v7 (library mode) with protected routes via `ProtectedRoute`
- Zustand auth store with `persist` middleware; access token excluded from `localStorage`
- Axios instance with request interceptor (attach Bearer token) and response interceptor (401 → auto-refresh → retry)
- `LoginPage` and `RegisterPage` with form validation and error display
- `AppShell` layout: `Sidebar` (dark, active NavLink highlighting) + `Header` (org name, role badge, user dropdown with logout)
- TypeScript types: `AuthResponse`, `UserInfo`, `OrgInfo`, `ApiResponse<T>`, `MemberRole`, `Plan`, `OrgStatus`

---

[Unreleased]: https://github.com/your-org/qalytix/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/your-org/qalytix/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/your-org/qalytix/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/your-org/qalytix/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/your-org/qalytix/releases/tag/v0.1.0
