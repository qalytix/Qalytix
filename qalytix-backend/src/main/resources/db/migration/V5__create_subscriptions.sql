-- Phase 5: Billing & Subscriptions
-- Stores the active subscription for each organization.
-- One row per org; updated in-place on plan changes.

CREATE TABLE subscriptions (
    id                  BIGSERIAL       PRIMARY KEY,
    org_id              BIGINT          NOT NULL UNIQUE REFERENCES organizations(id),

    -- Stripe identifiers (nullable until real keys are wired)
    stripe_customer_id      VARCHAR(100),
    stripe_subscription_id  VARCHAR(100),

    plan                VARCHAR(20)     NOT NULL DEFAULT 'FREE',
    status              VARCHAR(20)     NOT NULL DEFAULT 'TRIALING',
    billing_period      VARCHAR(10)     NOT NULL DEFAULT 'MONTHLY',

    current_period_start    TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    trial_ends_at           TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,

    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_org_id ON subscriptions(org_id);

-- Seed a FREE/TRIALING subscription for every existing org
INSERT INTO subscriptions (org_id, plan, status, billing_period)
SELECT id, 'FREE', 'ACTIVE', 'MONTHLY'
FROM   organizations;
