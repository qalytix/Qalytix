# Qalytix — Development Standards & Plan

## Product Context

Qalytix is a **SaaS** AI-powered Test Intelligence Platform. Engineering teams sign up, create an organization, connect their CI/CD pipelines, and get real-time test monitoring, failure analytics, and intelligent reporting — all scoped and isolated per organization.

**SaaS-first means every design decision must account for:**
- Multi-tenancy and strict data isolation between organizations
- Subscription tiers with feature gates and usage limits
- Self-service onboarding (signup → org creation → connect CI)
- Billing lifecycle (trial, paid, cancellation)
- Role-based access within an organization

---

## Monorepo Layout

```
Qalytix/
├── qalytix-backend/   # Spring Boot multi-tenant API
├── qalytix-ui/        # React SPA (tenant-aware)
└── qalytix-docs/      # Documentation (future)
```

---

## Tech Stack (Locked)

| Layer | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 4.0.6 |
| Auth | Spring Security + JWT (stateless) |
| Persistence | Spring Data JPA + PostgreSQL |
| DB migrations | Flyway |
| Real-time | Spring WebSocket (STOMP) |
| Billing | Stripe (subscriptions + webhooks) |
| Email | Spring Mail + SendGrid |
| Utility | Lombok, MapStruct |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Frontend language | TypeScript |
| Frontend framework | React 18 (Vite) |
| Styling | Tailwind CSS v4 |
| State management | Zustand |
| HTTP client | Axios |
| Charts | Chart.js + react-chartjs-2 |
| Routing | React Router v7 |
| Infrastructure | Docker + Nginx |

---

## Multi-Tenancy Architecture

**Strategy: Shared database, shared schema with `org_id` tenant isolation.**

- Every tenant-scoped entity has a non-nullable `org_id` FK column referencing the `organizations` table
- PostgreSQL Row-Level Security (RLS) enforces isolation at the DB level as a safety net
- A Spring `TenantContext` (thread-local) holds the current org ID, set by the JWT filter on every request
- All repository queries include `orgId` in their WHERE clause — no cross-tenant queries ever

**Tenant resolution flow:**
```
Request → JWT Filter → extract orgId from token claims → set TenantContext
→ Service → Repository → SQL always includes WHERE org_id = :currentOrgId
```

**Never:**
- Write a query that fetches data across all orgs (unless it's a super-admin endpoint)
- Skip the `orgId` parameter in any service method that touches tenant data
- Let entities from one org appear in another org's responses

---

## Organization & User Model

```
Organization
├── id, name, slug (unique URL-safe identifier), plan, status, trial_ends_at
└── created_at, updated_at

User
├── id, email, password_hash, name, email_verified, created_at
└── (global — a user can belong to multiple orgs)

OrganizationMember  (join table)
├── org_id, user_id, role (OWNER | ADMIN | MEMBER), invited_by, joined_at
└── status (PENDING | ACTIVE)

Invitation
├── id, org_id, email, role, token (UUID), expires_at, accepted_at
```

**Roles within an org:**
- `OWNER` — full control, billing, delete org
- `ADMIN` — manage members, integrations, notifications
- `MEMBER` — read-only access to analytics and dashboards

---

## Subscription Tiers

| Plan | Jenkins Connections | Users | Data Retention | Features |
|---|---|---|---|---|
| **Free** | 1 | 3 | 7 days | Basic dashboard |
| **Pro** | 5 | 15 | 90 days | Analytics, notifications |
| **Enterprise** | Unlimited | Unlimited | 1 year | All features, audit log, SSO |

- Plan limits are enforced in the `PlanGuard` service before any resource creation
- `Subscription` entity stores Stripe subscription ID, plan, billing period, status

---

## Backend Standards

### Package Structure

```
com.qalytix
├── config/            # Spring configs (Security, WebSocket, CORS, Stripe, Mail)
├── controller/        # REST controllers — no business logic
├── service/           # Business logic interfaces + implementations
├── repository/        # Spring Data JPA repositories
├── entity/            # JPA entities
├── dto/               # Request/Response DTOs
│   ├── request/
│   └── response/
├── mapper/            # MapStruct mappers
├── exception/         # Custom exceptions + GlobalExceptionHandler
├── security/          # JWT filter, UserDetails, TenantContext, SecurityUtils
├── integration/       # External clients (Jenkins, Teams, Stripe, SendGrid)
├── websocket/         # STOMP handlers and event publishers
├── tenant/            # TenantContext, TenantFilter, TenantAwareRepository base
├── billing/           # Stripe webhook handler, plan enforcement, PlanGuard
└── util/              # Shared utilities
```

### Naming Conventions

- Entities: `PascalCase`, no `Entity` suffix (`Build`, `TestRun`, `Organization`)
- DTOs: `CreateBuildRequest`, `BuildResponse`, `BuildSummaryResponse`
- Services: interface `BuildService`, implementation `BuildServiceImpl`
- Controllers: `BuildController` — `@RestController @RequestMapping("/api/v1/builds")`
- Repositories: `BuildRepository extends JpaRepository<Build, Long>`
- Mappers: `BuildMapper` — `@Mapper(componentModel = "spring")`

### API Design

- All authenticated endpoints: `/api/v1/**`
- Public/auth endpoints: `/api/v1/auth/**`
- Super-admin endpoints: `/api/v1/admin/**` (separate security filter chain)
- Consistent response envelope:
```json
{ "success": true, "data": {...}, "message": "OK", "timestamp": "..." }
```
- Errors use `GlobalExceptionHandler` with RFC 9457 `ProblemDetail`
- Pagination via Spring `Pageable` — `page`, `size`, `sort` query params
- Tenant context is resolved from JWT — never passed as a URL parameter

### Security

- JWT access token: 15 min expiry; refresh token: 7 days (stored in `refresh_tokens` table)
- JWT claims include: `userId`, `orgId`, `role`, `email`
- Tokens stored in HTTP-only cookies on the frontend; `Authorization: Bearer` accepted as fallback
- Public endpoints: `/api/v1/auth/**`, `/api/v1/invitations/accept`, `/actuator/health`, `/swagger-ui/**`, `/stripe/webhooks`
- `TenantContext` is always set before any service method executes
- Stripe webhook endpoint validates `Stripe-Signature` header before processing

### Database Rules

- All migrations: `src/main/resources/db/migration/V{n}__{description}.sql`
- Never use `spring.jpa.hibernate.ddl-auto=update`
- Entity IDs: `BIGSERIAL` primary keys
- All tenant-scoped entities: non-nullable `org_id BIGINT REFERENCES organizations(id)`
- All entities: `created_at TIMESTAMPTZ DEFAULT NOW()`, `updated_at TIMESTAMPTZ`
- Soft delete for org data: `deleted_at TIMESTAMPTZ NULL` — never hard-delete tenant data
- RLS policies defined in Flyway migrations as an extra safety net

### Code Quality Rules

- No business logic in controllers
- No direct repository calls in controllers
- Entities never cross the service boundary — DTOs only
- `@Transactional` on all service methods that write
- `PlanGuard.check(orgId, Feature.X)` called before any feature-gated operation
- No `System.out.println` — SLF4J via Lombok `@Slf4j`
- All `orgId` values sourced from `TenantContext` — never trusted from request body

---

## Frontend Standards

### Project Structure

```
qalytix-ui/src/
├── api/              # Axios instance + per-resource API functions
├── components/
│   ├── common/       # Button, Input, Modal, Badge, Spinner
│   ├── charts/       # Chart.js wrapper components
│   └── layout/       # Sidebar, Header, PageShell, OrgSwitcher
├── features/
│   ├── auth/         # Login, Register, AcceptInvitation
│   ├── onboarding/   # OrgCreate, ConnectJenkins wizard
│   ├── dashboard/    # Live dashboard
│   ├── analytics/    # Failure trends, flaky tests, module stats
│   ├── jenkins/      # Jenkins config management
│   ├── members/      # Team members, invitations, roles
│   ├── billing/      # Plan page, Stripe checkout, usage meters
│   ├── notifications/# Teams/Slack webhook config
│   └── reports/      # Historical reports, export
├── hooks/            # Custom React hooks
├── pages/            # Route-level page components
├── stores/           # Zustand stores
│   ├── authStore.ts
│   ├── orgStore.ts
│   └── uiStore.ts
├── types/            # TypeScript types and interfaces
└── utils/            # Helpers, formatters, constants
```

### Naming Conventions

- Components: `PascalCase.tsx`
- Hooks: `use` prefix (`useBuilds.ts`, `useWebSocket.ts`)
- Stores: `camelCase` + `Store` suffix (`authStore.ts`)
- API files: one per resource (`api/builds.ts`, `api/orgs.ts`)

### Component Rules

- Functional components only
- Props typed with inline `interface Props {}` or named `XxxProps`
- Tailwind utility classes only — no inline styles, no separate CSS files
- Extract logic into custom hooks; keep JSX focused on rendering

### State Management (Zustand)

- `authStore` — current user, JWT state, login/logout
- `orgStore` — current organization, plan, member role
- `uiStore` — sidebar state, notifications, modals
- API calls live in `api/` functions — stores call them, not the reverse

### Tenant Awareness in the Frontend

- After login, the JWT contains `orgId` — stored in `orgStore`
- If a user belongs to multiple orgs, an `OrgSwitcher` in the header triggers a token re-issue for the selected org
- All API calls automatically carry the org context via the JWT; no manual `orgId` injection needed in the frontend

### Styling (Tailwind v4)

- Design tokens (colors, spacing) defined in `tailwind.config.ts`
- Dark mode from day one using `dark:` variant
- Responsive layouts using `sm:`, `md:`, `lg:` breakpoints

---

## Development Phases

---

### Phase 1 — SaaS Foundation + Auth ✅
**Goal:** Multi-tenant auth, org creation, member invitation — the SaaS skeleton.

#### Backend
- [x] Folder structure and package skeleton
- [x] `application.yml` with `dev` and `prod` profiles
- [x] PostgreSQL + Flyway baseline
- [x] Migrations: `organizations`, `users`, `organization_members`, `refresh_tokens`, `invitations`
- [x] `TenantContext` (thread-local) and `TenantFilter`
- [x] JWT utility: generate (with `orgId`, `role` claims), validate, refresh
- [x] Spring Security config: stateless, JWT filter, public endpoints
- [x] `AuthController`: register, login, refresh, logout
- [x] `OrganizationController`: create org (on first signup), get current org
- [x] `MemberController`: list members, update role, remove member
- [x] `InvitationController`: send invite (email), accept invite (token)
- [x] Email service via SendGrid: invitation email template
- [x] Global exception handler (`ProblemDetail`)
- [x] SpringDoc OpenAPI setup
- [x] Docker Compose: `postgres` + `qalytix-backend`

#### Frontend
- [x] Vite + React 19 + TypeScript scaffold in `qalytix-ui/`
- [x] Tailwind CSS v4, React Router v7
- [x] Axios instance with JWT interceptor + 401 → refresh flow
- [x] `authStore`, `orgStore`
- [x] Login, Register pages
- [ ] Onboarding: create organization wizard (name, slug)
- [ ] Accept invitation page (token from email link)
- [x] App shell: sidebar, header with org name + user menu
- [ ] Team members page: list, invite, change role, remove
- [x] Protected route wrapper checking auth + org context

**Deliverable:** User signs up, creates org, invites a teammate — all auth flows work.

---

### Phase 2 — Jenkins Integration & Data Ingestion
**Goal:** Connect Jenkins, sync jobs and build history, scoped per org.

#### Backend
- [ ] Migrations: `jenkins_configs`, `jobs`, `builds`
- [ ] `JenkinsConfig` entity (org-scoped: URL, credentials, polling interval)
- [ ] `JenkinsClient` (`integration/`): fetch jobs list, fetch build details via Jenkins REST API
- [ ] `JenkinsIngestionService`: sync jobs + builds, upsert logic
- [ ] Scheduled polling per active Jenkins config (`@Scheduled` + configurable cron)
- [ ] `PlanGuard` check on Jenkins connection count before creation
- [ ] `JenkinsController`: CRUD for Jenkins configs, manual sync trigger, test connection
- [ ] `JobController`: list jobs (org-scoped), job detail
- [ ] `BuildController`: builds for a job, build detail

#### Frontend
- [ ] Jenkins settings page: add/edit server config, test connection button
- [ ] Jobs list page: all synced jobs with last build status badge
- [ ] Build history page for a selected job with pass/fail timeline

**Deliverable:** Org admin configures Jenkins, jobs and build history appear in the app.

---

### Phase 3 — Live Dashboard & Real-time Monitoring
**Goal:** Real-time visibility into active builds using WebSocket, per org.

#### Backend
- [ ] WebSocket config (STOMP over SockJS, `/ws` endpoint)
- [ ] Auth for WebSocket connections: validate JWT on CONNECT frame
- [ ] Org-scoped STOMP topics: `/topic/org/{orgId}/builds`
- [ ] `BuildEventPublisher`: broadcasts build status change events
- [ ] Hook ingestion events into publisher
- [ ] `DashboardController`: summary stats (active builds, today's totals, recent failures)

#### Frontend
- [ ] `useWebSocket` hook — connects with JWT, subscribes to org topic
- [ ] Live dashboard page:
  - Active/running builds widget (live updates)
  - Today's pass/fail/total counters
  - Recent build activity feed
- [ ] Build status badge component (running, passed, failed, unstable, aborted)

**Deliverable:** Dashboard live-updates as Jenkins builds run — no page refresh needed.

---

### Phase 4 — Test Analytics & Intelligence
**Goal:** Flaky test detection, failure trends, and module stability — the core value.

#### Backend
- [ ] Migration: `test_results` (individual test case outcomes per build)
- [ ] JUnit XML parser ingested from Jenkins build artifacts
- [ ] `AnalyticsService`:
  - Failure rate by job/module over a configurable time window
  - Flaky test detection (alternating pass/fail pattern, configurable threshold)
  - Top N failing tests ranking
  - Module-wise stability score
- [ ] `AnalyticsController`: REST endpoints with date-range + pagination support
- [ ] `PlanGuard` check: analytics data retention window gated by plan

#### Frontend
- [ ] Analytics dashboard page
- [ ] Failure trend line chart (Chart.js) — by day/week
- [ ] Flaky tests table with flakiness score, trend indicator
- [ ] Top failing tests bar chart
- [ ] Module stability table or heatmap
- [ ] Date range picker, job filter

**Deliverable:** Engineers identify unreliable tests and modules without digging through CI logs.

---

### Phase 5 — Billing & Subscription
**Goal:** Stripe integration, plan enforcement, self-service upgrade/downgrade.

#### Backend
- [ ] Migration: `subscriptions` (Stripe subscription ID, plan, status, period)
- [ ] `StripeService`: create customer, create checkout session, handle webhooks
- [ ] Stripe webhook handler (`/stripe/webhooks`): `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_failed`
- [ ] `PlanGuard` service: check feature access and usage limits against current plan
- [ ] `BillingController`: get current plan, create checkout session (upgrade), billing portal session

#### Frontend
- [ ] Billing settings page: current plan, usage meters (Jenkins connections, users, data retention)
- [ ] Upgrade plan page: plan comparison table + Stripe checkout redirect
- [ ] Plan limit banners: non-blocking warning when approaching limits, blocking prompt when at limit
- [ ] Post-checkout success/cancel handling

**Deliverable:** Orgs on Free can upgrade to Pro via Stripe; limits are enforced throughout the app.

---

### Phase 6 — Notifications (Teams & Slack)
**Goal:** Alert teams on build failures and flaky test thresholds.

#### Backend
- [ ] Migration: `notification_configs`, `notification_events`
- [ ] `TeamsNotificationService`: POST to Incoming Webhook with adaptive card payload
- [ ] `SlackNotificationService`: POST to Slack Incoming Webhook
- [ ] Trigger rules: build failed, N consecutive failures, flaky rate exceeded threshold
- [ ] `NotificationController`: CRUD for configs, test send, notification history

#### Frontend
- [ ] Notification settings page: add Teams/Slack webhooks, configure trigger rules
- [ ] Test notification button
- [ ] Notification history list

**Deliverable:** Teams/Slack receive formatted alerts when builds break or test quality degrades.

---

### Phase 7 — Historical Reports & Export
**Goal:** Exportable reports for retrospectives and stakeholder communication.

#### Backend
- [ ] `ReportController`: date-range filtered summary reports (per org, per job)
- [ ] CSV export endpoint
- [ ] PDF export (iText or JasperReports)
- [ ] `PlanGuard` check: extended date range gated by plan

#### Frontend
- [ ] Reports page: date picker, job/module filter
- [ ] Summary stats table: total runs, pass rate, avg duration, flaky count
- [ ] Export CSV / Export PDF buttons

**Deliverable:** Team leads download weekly/monthly test health reports.

---

### Phase 8 — Super-Admin Dashboard
**Goal:** Internal Qalytix ops — manage all tenants, subscriptions, and platform health.

#### Backend
- [ ] Separate `/api/v1/admin/**` security filter chain (SUPER_ADMIN role)
- [ ] `AdminController`: list all orgs, view org details, change plan manually, impersonate org
- [ ] Platform stats: total orgs, active subscriptions, build ingestion volume

#### Frontend
- [ ] Admin portal at `/admin` (separate route group, SUPER_ADMIN only)
- [ ] Org list with plan/status filters
- [ ] Org detail: members, Jenkins configs, subscription history

**Deliverable:** Qalytix team can manage and support tenants without direct DB access.

---

## Versioning Strategy

Qalytix uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`) across both apps. Backend and frontend always share the same version number.

### Version meaning

| Segment | When to bump |
|---|---|
| `MAJOR` | Breaking API changes or platform-wide architecture overhaul |
| `MINOR` | A new phase ships (Phase 2 → `0.2.0`, Phase 3 → `0.3.0`, …) |
| `PATCH` | Bug fixes or small improvements within a released version |

### Phase → version mapping

| Milestone | Version |
|---|---|
| Phase 1 — SaaS Foundation | `0.1.0` |
| Phase 2 — Jenkins Integration | `0.2.0` |
| Phase 3 — Live Dashboard | `0.3.0` |
| Phase 4 — Test Analytics | `0.4.0` |
| Phase 5 — Billing | `0.5.0` |
| Phase 6 — Notifications | `0.6.0` |
| Phase 7 — Reports | `0.7.0` |
| Phase 8 — Super-Admin | `0.8.0` |
| Full MVP shipped | `1.0.0` |

### Pre-release identifiers

| Stage | Backend (`pom.xml`) | Frontend (`package.json`) |
|---|---|---|
| In development | `0.x.0-SNAPSHOT` | `0.x.0-alpha` |
| Release candidate | `0.x.0-RC1` | `0.x.0-rc.1` |
| Released | `0.x.0` | `0.x.0` |

### Git tags

- Tag every release on `master`: `git tag -a v0.1.0 -m "Phase 1 — SaaS Foundation"`
- Tag format: `v{MAJOR}.{MINOR}.{PATCH}` — no suffixes on tags (tags always point to released commits)
- Push tags: `git push origin --tags`

### Changelog

- Maintain `CHANGELOG.md` at the repo root following [Keep a Changelog](https://keepachangelog.com) format
- Every PR must update `CHANGELOG.md` under `[Unreleased]`
- On release: rename `[Unreleased]` to `[0.x.0] - YYYY-MM-DD` and open a new empty `[Unreleased]` section

### Bumping versions (checklist)

When a phase is complete and ready to release:

1. Mark every checklist item in the completed phase section below as `[x]` in this file
2. Update `qalytix-backend/pom.xml`: remove `-SNAPSHOT`, e.g. `0.1.0`
3. Update `qalytix-ui/package.json`: remove `-alpha`, e.g. `0.1.0`
4. Update `CHANGELOG.md`: rename `[Unreleased]` → `[0.1.0] - YYYY-MM-DD`
5. Commit: `chore: release v0.1.0`
6. Tag: `git tag -a v0.1.0 -m "Phase 1 — SaaS Foundation"`
7. Push commit + tag
8. Immediately bump to next SNAPSHOT: `0.2.0-SNAPSHOT` / `0.2.0-alpha`
9. Commit: `chore: begin v0.2.0 development`

> **Rule:** The phase checklists in this file are the source of truth for implementation status. Always keep them up to date — tick items as they are completed, not only at phase end.

---

## Git Workflow

- Branch naming: `feature/<short-name>`, `fix/<short-name>`, `chore/<short-name>`
- Commit style: `feat: add Jenkins polling service`, `fix: correct tenant filter`
- One PR per phase deliverable minimum
- Never commit directly to `master`

---

## Running Locally

```bash
# Backend
cd qalytix-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd qalytix-ui
npm run dev

# Full stack
docker compose up --build
```

---

## Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password |
| `JWT_SECRET` | 256-bit secret for signing tokens |
| `JWT_EXPIRY_MS` | Access token TTL (default 900000) |
| `STRIPE_SECRET_KEY` | Stripe API key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `SENDGRID_API_KEY` | SendGrid API key for transactional email |
| `APP_BASE_URL` | Public URL (used in email links, e.g. `https://app.qalytix.io`) |
| `JENKINS_POLL_INTERVAL` | Cron expression for Jenkins polling |
