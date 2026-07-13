-- ============================================================================
-- V17__careers.sql
-- Careers module: moderated job postings + public applications.
--
-- Seeds the 3 postings that previously lived in
-- apps/website/content/careers.json so the marketing careers page
-- returns real data on first deploy.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS careers;

CREATE TABLE IF NOT EXISTS careers.job_postings (
    id                  UUID PRIMARY KEY,
    slug                VARCHAR(120) NOT NULL UNIQUE,
    title               VARCHAR(200) NOT NULL,
    team                VARCHAR(80) NOT NULL,
    location            VARCHAR(200) NOT NULL,
    employment_type     VARCHAR(40) NOT NULL,
    commitment          VARCHAR(60),
    summary             TEXT NOT NULL,
    responsibilities    JSONB NOT NULL DEFAULT '[]'::jsonb,
    requirements        JSONB NOT NULL DEFAULT '[]'::jsonb,
    salary_range_min    BIGINT,
    salary_range_max    BIGINT,
    currency            CHAR(3),
    status              VARCHAR(20) NOT NULL DEFAULT 'draft',
    posted_at           DATE,
    published_at        TIMESTAMPTZ,
    archived_at         TIMESTAMPTZ,
    created_by_user_id  UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    extensions          JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_job_postings_status CHECK (status IN ('draft', 'published', 'archived'))
);

CREATE INDEX IF NOT EXISTS ix_job_postings_status_published
    ON careers.job_postings (status, published_at DESC);
CREATE INDEX IF NOT EXISTS ix_job_postings_slug
    ON careers.job_postings (slug);

CREATE TABLE IF NOT EXISTS careers.job_applications (
    id                    UUID PRIMARY KEY,
    job_posting_id        UUID NOT NULL REFERENCES careers.job_postings(id) ON DELETE CASCADE,
    applicant_name        VARCHAR(200) NOT NULL,
    applicant_email       VARCHAR(320) NOT NULL,
    applicant_phone       VARCHAR(40),
    resume_url            TEXT,
    cover_letter          TEXT NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'new',
    reviewer_note         TEXT,
    submitted_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by_user_id   UUID,
    reviewed_at           TIMESTAMPTZ,
    source_ip             VARCHAR(64),
    extensions            JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_job_applications_status CHECK (status IN ('new', 'reviewing', 'contacted', 'rejected', 'hired'))
);

CREATE INDEX IF NOT EXISTS ix_job_applications_job_submitted
    ON careers.job_applications (job_posting_id, submitted_at DESC);

CREATE TABLE IF NOT EXISTS careers.outbox (
    id                UUID PRIMARY KEY,
    event_type        VARCHAR(120) NOT NULL,
    event_version     INT NOT NULL,
    aggregate_type    VARCHAR(60) NOT NULL,
    aggregate_id      UUID NOT NULL,
    payload           JSONB NOT NULL,
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at      TIMESTAMPTZ,
    trace_id          VARCHAR(80)
);
CREATE INDEX IF NOT EXISTS ix_careers_outbox_unpublished
    ON careers.outbox (published_at, occurred_at)
    WHERE published_at IS NULL;

-- ------------------------------------------------------------------
-- Seed the 3 hard-coded roles from apps/website/content/careers.json
-- so the marketing careers page returns real data on first deploy.
-- ------------------------------------------------------------------
INSERT INTO careers.job_postings (
    id, slug, title, team, location, employment_type, commitment,
    summary, responsibilities, requirements, status, posted_at, published_at
) VALUES
(
    '11111111-1111-4111-8111-111111111111',
    'senior-fullstack-engineer',
    'Senior Full-Stack Engineer',
    'Engineering',
    'Remote — Global (India / EU / Americas overlap)',
    'Full-time',
    '40h / week',
    'Lead technical delivery on multi-quarter engagements across finance, health, and logistics clients. TypeScript + one strong backend language.',
    '["Own the technical shape of engagements from discovery to handoff", "Architect and ship platforms that survive their first re-org", "Mentor two to three engineers and set the bar for craft"]'::jsonb,
    '["6+ years shipping production systems", "Deep TypeScript. One of Kotlin / Go / Elixir / Rust at production depth.", "Comfort with ambiguity — engagements start before the spec exists"]'::jsonb,
    'published',
    DATE '2026-06-14',
    TIMESTAMPTZ '2026-06-14 00:00:00Z'
),
(
    '11111111-1111-4111-8111-111111111112',
    'brand-designer',
    'Brand Designer',
    'Design',
    'Bengaluru or Remote (EU / GCC overlap)',
    'Full-time',
    '40h / week',
    'Identity work for boutique clients across finance, wealth, and technology. Typography-first. Craft over speed.',
    '["Lead identity engagements from positioning to guidelines", "Work with type designers and photographers we collaborate with", "Present directly to founders and boards"]'::jsonb,
    '["5+ years brand identity work — portfolio speaks first", "Fluency in typography, colour theory, and print production", "Comfort presenting work to executives"]'::jsonb,
    'published',
    DATE '2026-06-01',
    TIMESTAMPTZ '2026-06-01 00:00:00Z'
),
(
    '11111111-1111-4111-8111-111111111113',
    'growth-strategist',
    'Growth Strategist',
    'Growth',
    'London or Remote (Americas overlap)',
    'Full-time',
    '40h / week',
    'Run performance marketing engagements for DTC and B2B clients. Carry a P&L. Own attribution the CFO can defend.',
    '["Own CAC / LTV / MER on client accounts spending $500k–$5M/mo", "Design and run incrementality studies", "Present to CMOs and CEOs weekly"]'::jsonb,
    '["5+ years managing paid media spend at scale", "Fluency across Meta, Google, TikTok, LinkedIn — and the measurement stack around them", "Comfort with SQL, Python, or dbt"]'::jsonb,
    'published',
    DATE '2026-05-20',
    TIMESTAMPTZ '2026-05-20 00:00:00Z'
)
ON CONFLICT (slug) DO NOTHING;

-- Note: permissions are seeded per-user at creation time via
-- PermissionCatalog (not a DB table). PermissionCatalog was updated to
-- grant `careers:*` to admin + PM roles in the same commit as this
-- migration. Existing users must have careers permissions granted
-- manually (INSERT INTO identity.user_permissions).

-- ROLLBACK:
-- DROP SCHEMA IF EXISTS careers CASCADE;
