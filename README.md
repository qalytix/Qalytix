# Qalytix

<p align="center">
  AI-Powered Test Intelligence Platform
</p>

<p align="center">
Monitor • Analyze • Predict
</p>

---

## Overview

Qalytix is a **SaaS** test intelligence platform that transforms CI/CD execution data into actionable insights for engineering teams.

Teams sign up, connect their Jenkins pipelines, and instantly get:

- Real-time build and test execution monitoring
- Failure trend analysis across jobs and modules
- Flaky test detection with confidence scoring
- Historical analytics and exportable reports
- Alerts via Microsoft Teams and Slack

Qalytix helps teams move beyond static CI reports toward data-driven testing decisions.

---

## Vision

Traditional CI reports tell teams **what happened**.

Qalytix answers:

- What happened?
- Why did it happen?
- Will it happen again?
- What should be fixed first?

---

## SaaS Model

Qalytix is a multi-tenant SaaS product. Each organization gets an isolated workspace with:

- Role-based team access (Owner, Admin, Member)
- Self-service onboarding and org management
- Subscription-based plans with usage limits
- Stripe billing and self-service upgrades

### Plans

| Plan | Jenkins Connections | Users | Data Retention |
|---|---|---|---|
| Free | 1 | 3 | 7 days |
| Pro | 5 | 15 | 90 days |
| Enterprise | Unlimited | Unlimited | 1 year |

---

## Planned Features

### Live Monitoring
- Real-time execution dashboard
- Running build and job status
- Execution duration tracking

### Test Intelligence
- Failure trend analysis
- Flaky test detection
- Module-wise stability scoring
- Root cause insights

### Notifications
- Microsoft Teams integration
- Slack integration
- Configurable alert rules

### Reporting
- Historical execution reports
- Success/failure trends
- Exportable CSV and PDF reports

---

## MVP (Version 0.1)

- [ ] Multi-tenant auth (signup, org creation, team invitations)
- [ ] Jenkins integration and data ingestion
- [ ] Live dashboard with WebSocket real-time updates
- [ ] Failure trend analytics and flaky test detection
- [ ] Stripe billing and plan enforcement
- [ ] Microsoft Teams and Slack notifications
- [ ] Historical reports and export

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 4.0.6
- Spring Security (JWT, stateless)
- Spring Data JPA
- PostgreSQL (multi-tenant, shared schema)
- Flyway (migrations)
- Spring WebSocket (STOMP)

### Frontend
- React 18 + TypeScript (Vite)
- Tailwind CSS v4
- Zustand
- Chart.js

### Billing & Communication
- Stripe
- SendGrid

### Infrastructure
- Docker + Nginx

### Integrations
- Jenkins
- Microsoft Teams
- Slack
- (Future: GitHub, GitLab, Selenium, Cucumber)

---

## Project Structure

```text
Qalytix/
├── qalytix-backend/   # Spring Boot multi-tenant API
├── qalytix-ui/        # React SPA
└── qalytix-docs/      # Documentation
```

---

## Development Status

**Current Phase:** MVP Planning — Phase 1 (SaaS Foundation + Auth) starting next.

See [CLAUDE.md](CLAUDE.md) for the full phase-by-phase development plan and coding standards.

---

## Future Roadmap

- AI-based failure prediction
- Intelligent test prioritization recommendations
- GitHub / GitLab CI integration
- SSO (SAML/OIDC) for Enterprise
- Plugin and webhook ecosystem
- Advanced analytics engine

---

## License

MIT License

---

Built with passion for smarter testing.
