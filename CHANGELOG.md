# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Phase 1 — Foundation & Authentication (Backend: `0.1.0-SNAPSHOT`, Frontend: `0.1.0-alpha`)

#### Backend

**Added**
- Multi-tenant data model: `organizations`, `users`, `organization_members`, `invitations`, `refresh_tokens` tables via Flyway migration `V1__create_organizations_and_users.sql`
- Entities: `Organization`, `User`, `OrganizationMember`, `Invitation`, `RefreshToken` with full JPA mappings
- Enums: `Plan` (FREE/PRO/ENTERPRISE), `OrgStatus`, `MemberRole` (OWNER/ADMIN/MEMBER), `MemberStatus`
- JWT authentication: stateless, claims include `userId`/`orgId`/`email`/`role`; access tokens in-memory, refresh tokens stored as SHA-256 hash
- `JwtUtil` — token generation and validation (JJWT 0.12.x)
- `JwtAuthFilter` — `OncePerRequestFilter`; populates `SecurityContext` + `TenantContext`; always clears both in `finally`
- `TenantContext` — `ThreadLocal<Long>` for current org scoping; prevents cross-tenant data leaks
- `AuthenticatedUser` — custom `Principal` record injected via `@AuthenticationPrincipal`
- `SecurityConfig` — stateless `SecurityFilterChain`; public endpoints: `/api/v1/auth/**`, `/api/v1/invitations/accept`, `/actuator/health`, Swagger UI
- `AuthService` — register (creates User + Organization + OWNER membership atomically), login, token refresh, logout (revokes all refresh tokens for user+org)
- `OrgService` — fetch current org details
- `MemberService` — list members; update role (OWNER-only rules, last-OWNER guard); remove member (self-removal guard, cross-OWNER guard)
- `InvitationService` — send invite (dedup, 7-day expiry, email stub); accept invite (existing user join or new user registration)
- REST controllers: `AuthController`, `OrgController`, `MemberController`, `InvitationController`
- `GlobalExceptionHandler` — RFC 9457 `ProblemDetail` responses; field-level validation errors as `errors` map
- `ApiResponse<T>` — unified response envelope
- `AppProperties` — `@ConfigurationProperties(prefix="app")` for JWT secret and expiry config
- Unit tests: `JwtUtilTest` (6), `AuthServiceImplTest` (11), `MemberServiceImplTest` (11), `InvitationServiceImplTest` (10)

#### Frontend

**Added**
- React 19 + TypeScript + Vite scaffold
- Tailwind CSS v4 with `@theme` custom color tokens
- React Router v7 (library mode) with protected routes via `ProtectedRoute`
- Zustand auth store with `persist` middleware; access token excluded from `localStorage`
- Axios instance with request interceptor (attach Bearer token) and response interceptor (401 → auto-refresh → retry)
- `LoginPage` and `RegisterPage` with form validation and error display
- `AppShell` layout: `Sidebar` (dark, active NavLink highlighting) + `Header` (org name, role badge, user dropdown with logout)
- Placeholder pages: Dashboard, Analytics, Jenkins, Members, Notifications, Reports, Billing
- TypeScript types: `AuthResponse`, `UserInfo`, `OrgInfo`, `ApiResponse<T>`, `MemberRole`, `Plan`, `OrgStatus`

---

[Unreleased]: https://github.com/your-org/qalytix/compare/v0.1.0...HEAD
