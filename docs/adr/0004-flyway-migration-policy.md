# ADR-0004 — Flyway migration policy

- Status: Accepted
- Date: 2026-07-12
- Deciders: Backend
- Supersedes: —
- Related: A-06, A-07 (audit remediation)

## Context

The audit surfaced two related issues in how database migrations are being authored:

- **A-06:** Migrations were being written as if the target DB were empty. Several bootstrap `INSERT`s in `V13__mfa_methods.sql` would fail on re-application against a partially seeded environment.
- **A-07:** A future migration that renamed or dropped a column would break rolling deploys because the previous app version is still writing to that column while the new one is coming up.

Flyway is our only DDL entry point. There is no ORM auto-DDL, no
schema-diff tool, no manual `psql` in prod. The policy below is what
every future migration in `src/main/resources/db/migration/` must
follow.

## Decision

### 1. Numbering & naming

- Sequential Vn: `V<number>__<snake_case_description>.sql`.
- `<number>` is strictly monotonic across all authors — no gaps, no re-use, no branch-local renumbering. If a rebase collides on a number, the later PR renumbers.
- One logical change per file. Do not bundle "add a table" + "backfill" + "drop a column" in the same V file.

### 2. Idempotency of bootstrap data

- Every seed `INSERT` uses `ON CONFLICT ... DO NOTHING` (or `DO UPDATE` when the row must converge). This makes `flyway repair` + re-apply safe.
- Every `CREATE TABLE / INDEX / SEQUENCE` uses `IF NOT EXISTS`.
- Every `DROP` uses `IF EXISTS`. Never `DROP TABLE foo CASCADE` from a migration — cascade is a human decision.

### 3. Additive-only inside a release

Rolling deploys mean version N and version N+1 both talk to the same DB
for a window. So inside a single release:

- **Allowed:** `CREATE TABLE`, `CREATE INDEX CONCURRENTLY`, `ADD COLUMN ... NULL`, `CREATE SEQUENCE`, backfilling a new NULL column.
- **Not allowed:** `DROP COLUMN`, `DROP TABLE`, `ALTER COLUMN ... NOT NULL` on a column the old code still writes NULL to, `RENAME COLUMN`, changing a column type in place, tightening a `CHECK` constraint on live data.

Renames + drops go through the **expand → migrate → contract** shape,
one release per step:

1. **Expand:** add the new column/table alongside the old one. Both versions of the app work.
2. **Migrate:** the new app version dual-writes to old + new. A background job (or a subsequent migration) backfills history. The old app version keeps reading the old column.
3. **Contract:** in a later release — never the same one — drop the old column, once all running instances are on the version that no longer needs it.

The contract step must be a separate PR whose description explicitly
names the release in which the expand step landed.

### 4. Long-running DDL

- Any `CREATE INDEX` on a table larger than ~1M rows must be `CREATE INDEX CONCURRENTLY`. Flyway will run it outside its own transaction (mark the migration `-- flyway:executeInTransaction=false`).
- `ALTER TABLE ... ADD COLUMN ... DEFAULT <expr>` is banned in prod for tables larger than ~1M rows — it rewrites the whole table. Add the column NULL, backfill in batches, then apply the default in a follow-up.
- Migrations that lock a hot table must ship with a rollback note in the PR description.

### 5. Reversibility

- Every migration ships with a `-- ROLLBACK:` block in the file (as a comment) describing the manual `psql` steps to reverse it. This is documentation, not automation — Flyway does not roll back. But when a prod migration goes wrong, the SRE reading the file at 3am should not have to derive the reverse.
- If a migration is genuinely irreversible (dropping a table containing customer data), the PR description must call this out and the change must be reviewed by a second engineer.

### 6. Data migrations

- Data migrations that touch more than ~10k rows do not live in Flyway — they live as an idempotent app-level job behind a feature flag. Flyway is for schema, not for hour-long backfills that would block the app boot.
- Small data fixups (seed data, permission grants) are fine in Flyway.

### 7. Prod parity in CI

- CI runs Flyway against a fresh Postgres 14 (Testcontainers) on every PR. The build fails if any migration fails to apply cleanly, or if the resulting schema drifts from the JPA metamodel more than tolerated by `hibernate.ddl-auto=validate`.
- `flyway validate` also runs against a clone of prod's schema history nightly (once prod exists) so a migration authored against a stale dev DB is caught before deploy.

## Consequences

- Schema changes get slower — a rename now takes at least two releases.
- Rolling deploys stop breaking.
- On-call has a written recipe for reversing a migration.
- Bootstrap seeds no longer fight `flyway repair` after partial failures.

## Alternatives considered

- **Liquibase.** Rejected — Flyway's forward-only model matches how we
  actually operate. Liquibase's rollback support is real but rarely
  used in practice, and its changelog XML is heavier than plain SQL.
- **ORM-managed schema.** Rejected — Hibernate `ddl-auto=update` has
  no notion of expand/contract, no rollback story, and produces
  different DDL depending on driver version. Every serious deploy the
  team has ever done ends up outside Hibernate anyway.
- **Manual DBA-run scripts.** Rejected — no audit trail, no CI
  verification, no dev/prod parity.

## Related migrations

- `V13__mfa_methods.sql` — reference for the `ON CONFLICT DO NOTHING` bootstrap pattern (post A-05 fix).
- `V14__notifications_event_dedup.sql` — reference for `ADD COLUMN IF NOT EXISTS` + `CREATE UNIQUE INDEX IF NOT EXISTS` (partial index).
- `V15__finance_invoice_seq.sql` — reference for `CREATE SEQUENCE IF NOT EXISTS`.
