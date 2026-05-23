-- ============================================================
-- V1: Organizations and Users
-- Core multi-tenant foundation: every subsequent table ties
-- back to organizations via org_id.
-- ============================================================

CREATE TABLE organizations (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)        NOT NULL,
    slug          VARCHAR(60)         NOT NULL UNIQUE,
    plan          VARCHAR(20)         NOT NULL DEFAULT 'FREE',
    status        VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
    trial_ends_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON organizations(slug);

-- ---------------------------------------------------------------

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    email            VARCHAR(255)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255)  NOT NULL,
    full_name        VARCHAR(150)  NOT NULL,
    email_verified   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ---------------------------------------------------------------

CREATE TABLE organization_members (
    id          BIGSERIAL PRIMARY KEY,
    org_id      BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    invited_by  BIGINT       REFERENCES users(id),
    joined_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (org_id, user_id)
);

CREATE INDEX idx_org_members_org_id  ON organization_members(org_id);
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);

-- ---------------------------------------------------------------

CREATE TABLE invitations (
    id           BIGSERIAL PRIMARY KEY,
    org_id       BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    token        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    invited_by   BIGINT       NOT NULL REFERENCES users(id),
    expires_at   TIMESTAMPTZ  NOT NULL,
    accepted_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_token  ON invitations(token);
CREATE INDEX idx_invitations_org_id ON invitations(org_id);

-- ---------------------------------------------------------------

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    org_id      BIGINT       NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
