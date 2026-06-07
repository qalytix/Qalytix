# Qalytix — AI-Powered Test Intelligence Platform

Monitor your CI/CD test quality, detect flaky tests, track failure trends, and alert your team — all in one place.

![Qalytix demo](https://raw.githubusercontent.com/qalytix/Qalytix/master/assets/demo.gif)

## Quick Start

**1. Create a `.env` file:**
```env
JWT_SECRET=your-random-secret-min-32-chars
APP_BASE_URL=http://localhost
```

**2. Create `docker-compose.yml`:**
```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: qalytix
      POSTGRES_USER: qalytix
      POSTGRES_PASSWORD: qalytix
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U qalytix"]
      interval: 10s
      retries: 5

  app:
    image: sddmhossain/qalytix:latest
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/qalytix
      DB_USERNAME: qalytix
      DB_PASSWORD: qalytix
      JWT_SECRET: ${JWT_SECRET}
      APP_BASE_URL: ${APP_BASE_URL:-http://localhost}
    ports:
      - "80:80"

volumes:
  postgres_data:
```

**3. Run:**
```bash
docker compose up -d
```

**4. Open** http://localhost — register and connect your Jenkins.

---

## Features

- **Live Dashboard** — real-time build status via WebSocket
- **Test Analytics** — failure trends, flaky test detection, module stability
- **Historical Reports** — date-range reports with CSV export
- **Notifications** — Microsoft Teams & Slack webhook alerts
- **Billing** — Free / Pro / Enterprise plan tiers (Stripe-ready)
- **Team Management** — invite members, role-based access
- **Super-Admin Portal** — manage all tenants and override plans

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | ✅ | Random secret, min 32 chars |
| `DB_URL` | ✅ | PostgreSQL JDBC URL |
| `DB_USERNAME` | ✅ | Database user |
| `DB_PASSWORD` | ✅ | Database password |
| `APP_BASE_URL` | ✅ | Public URL (used in email links) |
| `SENDGRID_API_KEY` | Optional | For invitation & password reset emails |
| `STRIPE_SECRET_KEY` | Optional | For paid plan billing |

## Source Code

https://github.com/qalytix/Qalytix
