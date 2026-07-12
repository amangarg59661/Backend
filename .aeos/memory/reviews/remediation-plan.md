# Self_Backend_v1 — Remediation Plan

**Status (Phase D closeout, 2026-07-12):** 12 fix batches landed on `audit-remediation`. Wave 1 code-fix: 16/16 done. Wave 2 code-fix: 24/33 done (remaining are Wave 3 client-owned or design refactor). 44 unit tests green, Modulith verify green. Commit index in `risk-register.md` "Phase D — Remediation Status" section.

Baseline: `7ba4e12` on `main`. Working branch: `audit-remediation`. Compiled from `risk-register.md`.

The engagement's commitment is full remediation. 112 deduplicated findings across four dimensions is a very wide surface, and the AEOS review pipeline requires every fix to pass peer / senior / architect / security / QA / documentation review before merge (`config/workflows.yaml`). Honouring both the commitment and the review pipeline means we are honest with the client about what a single engagement window can safely land vs. what belongs on a client-owned backlog with explicit sign-off — per `identity.yaml`, an unhonoured promise is a greater failure than a delayed one.

## Realistic scope for this engagement window

Everything in the **Wave 1** and **Wave 2** blocks below lands in `audit-remediation`, one commit per finding or per closely-coupled cluster. Each fix passes the review pipeline in-line (specialists batched into one review session per fix, per proportionality clause of workflows.yaml, with every checklist answered). After Wave 2 lands, we open the executive report and hand the residual backlog to the client for a Wave 3 planning conversation — the client owns the timeline for compliance work that needs legal counsel + product decisions.

### Wave 1 — Code-fixable Sev-1 findings without external dependencies

Focus: security + architecture + operations changes that land inside the repo without needing legal counsel, new vendor accounts, or product-side decisions from the client.

Ordered by fix sequence (dependency order):

1. **S-01 / G-04** — Stop persisting raw reset + invite tokens in outbox payload. Move to notification-id handles; the notification layer looks up the token from a short-lived Redis / in-memory cache. Purge job on outbox for extra safety.
2. **S-03** — Gate `X-Forwarded-For` behind a `TRUSTED_PROXY_CIDRS` env var; default empty. Take right-most trusted hop. Update `HttpRequests.clientIp` + rate limiter callers.
3. **S-07** — Add rate limits on `/auth/2fa/verify`, `/auth/2fa/whatsapp/send`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`. Reuse `RateLimiter` port.
4. **S-02** — Rate-limit WhatsApp OTP send: per-user `3/hour`, per-phone `5/day`; add daily global spend guard.
5. **S-05** — Password change + reset revoke every non-current session + refresh token for the user. Add token-version bump on password change.
6. **S-04** — Refresh flow: `SELECT ... FOR UPDATE` on session; on repeated use of the same token, revoke the whole session and family.
7. **S-06** — Sanitise Google OAuth error bodies before Sentry capture; strip response bodies from `IllegalStateException` before rethrow.
8. **S-08** — `admin:*` no longer grants across every scope. Split into `admin:read` (unchanged) + `admin:override` (explicit, logged at WARN on use).
9. **S-09** — `.env` remains gitignored; add pre-commit hook (`gitleaks` action + `.gitignore` audit) and `.dockerignore` guard against future accidents.
10. **A-01** — OutboxRelay documented ordering matches actual semantics. Landed today: publish-after-commit stays; add `event_id` idempotency contract in Javadoc + add `notifications.notifications UNIQUE (user_id, event_id)` migration to enforce it downstream (fixes A-16 at the same time).
11. **A-02** — Replace `AtomicLong` invoice sequence with a Postgres `SEQUENCE` per year; format `INV-{yyyy}-{padded}`.
12. **A-03 + A-04** — WebSocket CORS reads from `SecurityProperties.cors().allowedOrigins()`. New `StompAuthInterceptor` extracts the `Authorization` bearer from CONNECT, validates JWT, sets `Principal` — closes cross-user notification leakage.
13. **A-05** — V13 migration: add `ON CONFLICT (user_id, method_type) DO NOTHING`.
14. **A-06 + A-07** — Migration rollback policy: destructive migrations ship with a companion `V##U__…sql` documenting the reversal, or the ADR must acknowledge irreversibility. V13 documented as one-way with rationale; ADR-0004 lands.
15. **PF-05 + PF-06** — In-memory rate limiter + refresh + 2FA + WhatsApp OTP stores gain `@Scheduled` sweep tasks.
16. **PF-07** — HikariCP `maximum-pool-size` raised to 30 in prod profile; leak detection at 30s. ADR records the reasoning.

### Wave 2 — Code-fixable Sev-2 findings that share plumbing with Wave 1

Ordered by proximity to Wave 1 fixes.

17. **S-11** — WhatsApp OTP verify uses constant-time compare; invalidates OTP after 3 failed attempts.
18. **S-13** — MFA disable requires a live second-factor challenge in addition to the password.
19. **S-14** — verifyTwoFactor peeks the challenge, verifies, consumes on success only.
20. **S-15** — TOTP enrol response drops raw base32 secret; only otpauth URI + QR.
21. **S-16** — Cal.com webhook grows a `t=` timestamp scheme + 5-min tolerance; rejects older.
22. **S-18** — JwtService requires HS256 explicitly; `requireIssuer` / `requireAudience`; `clockSkewSeconds(30)`. Issuer + audience added to token issuance.
23. **S-19** — Google OAuth `state` stored under `state → userId` for 5 min; callback verifies + deletes; PKCE landing pad.
24. **S-24** — CORS registered on `/api/v1/**` only, minus webhooks + actuator.
25. **S-10** — SecretCipher gains 1-byte key-id prefix; supports old + new key during rotation window; ADR-0005.
26. **S-12** — Backup codes migrate to BCrypt(10); repository + service updated; existing SHA-256 codes are re-hashed on next login attempt as a one-shot migration path.
27. **A-08** — CursorCodec wired into `ProjectController.list`, `InvoiceController.list`, `NotificationController.list`, `TicketController.list`, `FileController.list`.
28. **A-09** — `extensions JSONB` mapped on entities as `Map<String, Object>` via Hibernate `@JdbcTypeCode(SqlTypes.JSON)`.
29. **A-10** — All entity PK creations switch to `UuidCreator.getTimeOrderedEpoch()`. Existing rows unaffected.
30. **A-11** — Enum CHECK constraints added for `finance.invoices.status`, `commitments.tickets.status`, `commitments.tickets.priority`, `projects.projects.status`, and equivalents. V14 migration.
31. **A-16** — `notifications.notifications UNIQUE (user_id, event_id)` migration + `InAppChannel.deliver` uses `ON CONFLICT DO NOTHING`.
32. **A-19** — OutboxRelay comment corrected.
33. **A-26** — Stripe API key passed via `RequestOptions.builder().setApiKey(...)` per call; JVM-global static removed.
34. **PF-01** — Adaptive backoff on outbox pollers (250 ms → 500 → 1000 → cap 2 s on empty batches; reset on non-empty).
35. **PF-03** — NotificationDispatcher fanout via `@Async("notificationExecutor")` on a bounded `ThreadPoolTaskExecutor`.
36. **PF-08 + PF-09** — Per-request timeouts (5 s) on Supabase / Twilio / Google Calendar HTTP clients.
37. **PF-10** — InvoiceService.issue splits: persist invoice + emit `InvoiceIssued` in tx; Stripe session created AFTER commit via a `@TransactionalEventListener(AFTER_COMMIT)`.
38. **PF-12** — Nightly purge job on `finance.payment_webhook_events` + `integrations.calendar_webhook_events` where `status = 'applied' AND received_at < now() - INTERVAL '90 days'`.
39. **CI-01** — Testcontainers image caching in the GitHub Actions workflow.
40. **CI-02** — Dependabot config for maven + gh-actions weekly; OWASP dep-check in a nightly-only job.
41. **CI-03** — SpotBugs + find-sec-bugs in `mvn verify`.
42. **CI-07** — `gitleaks` action on push + PR; `pre-commit` config file committed for local guardrail.
43. **O-01** — `micrometer-registry-prometheus` dep + `management.endpoints.web.exposure.include: health,info,prometheus,metrics` in prod; secured via management port + basic auth.
44. **O-03** — `logstash-logback-encoder` with a JSON appender enabled by profile.
45. **O-04** — `HealthIndicator` beans per enabled integration (Redis, Twilio, Stripe, Supabase, S3); registered only when the corresponding feature flag is on.
46. **T-01** — JaCoCo coverage gate at 60% line/module for `application` + `domain` packages.
47. **T-04** — Contract-shape test derived from committed JSON schemas (converted from frontend Zod).
48. **T-07** — Unit tests for `BackupCodeService.consume` single-use, `PasswordHistoryService.rejectIfReused`, `ProjectPhaseTransition` guard rejection.

### Wave 3 — Client-owned backlog (documented in the executive report)

These are legitimate findings. They will not land in this engagement window because they require decisions or artefacts outside the codebase. Deferring them silently would violate identity's honesty rule; they are declared explicitly here and will be surfaced in the executive report with owner + effort estimates.

**Compliance foundation (requires legal counsel + product decision)**
- G-01 / D-01 / D-05 consent model (data-model + product decisions on which purposes require which consent).
- G-02 / D-02 DSAR export endpoint (product decides bundle scope + delivery channel).
- G-03 / D-02 right-to-erasure (product decides retention exceptions per aggregate).
- G-05 / P-03 HTTPS + HSTS boundary (needs deployment topology decision — reverse proxy vs. Spring termination).
- G-06 / D-04 breach notification SOP (organisational, not code).
- D-03 grievance officer designation.
- G-09 / G-10 sub-processor register + DPAs.
- P-01 written PAN-absence statement from Stripe & Razorpay.
- P-04 annual SAQ-A self-attestation.
- L-01…L-06 legal counsel reviews.

**Product decisions**
- D-06 age gate — depends on target market.
- G-11 privacy notice link on public inquiry — needs product copy.
- G-12 DTO on submit shape — needs product decision on visible feedback.
- A-17 Anemic domain refactor — behaviour-neutral shuffle; needs its own architectural sprint.
- A-18 Contract HMAC signing — needs legal decision on evidentiary standard.

**Non-trivial testing infrastructure**
- T-02 integration tests for 2FA / webhook / refresh / ownership (Wave 1 makes them feasible; suite of eight IT files is its own sprint).
- T-05 load testing (k6/Gatling script + separate CI job).
- T-06 chaos test for outbox at-least-once.
- T-08 per-module slice tests.

**Nice-to-have polish**
- G-13 session IP retention job.
- G-14 documented rationale (backup code SHA-256).
- G-16 DPO / EU rep designation.
- A-20 idempotency contract Javadoc (touched partly by Wave 1's A-01 fix).
- A-21 Hikari sizing per profile (Wave 1 landed the base bump; per-profile tuning after load test).
- A-22 batch settings tuning.
- A-23 index consolidation.
- A-24 unused integrations outbox — decide keep-or-drop.
- A-25 Redis flag-flip warn.
- A-27 permission evaluator reflection cache.
- A-28 OpenAPI `@Operation` completeness (13+ endpoints; own sprint).
- A-29 project CASCADE choice.
- A-30…A-34 Sev-4 items.
- PF-04 BCrypt profiling.
- PF-11 partial index conversion.
- PF-13 line_items typed conversion.
- O-02 distributed tracing.
- O-05 business metrics.
- O-06 SLO doc.
- CI-04 container image + Trivy.
- CI-05 release step.
- CI-06 branch protection.
- CI-08 formatter.

## Execution rhythm inside the window

- One commit per Wave 1 / Wave 2 finding or tight cluster. Message references the finding id.
- Each commit runs the review pipeline before merge; specialists batched into one review session per commit per proportionality clause.
- ADRs recorded in `.aeos/memory/adr/` as decisions land — one per keyed decision, not per file.
- After Wave 1 lands: fast `mvn verify` + ModuleStructureTest re-run. Stop and report if either regresses.
- After Wave 2 lands: full re-review (Phase E). Executive report opens Phase F.

## Client checkpoint

The client already approved "Report + full remediation" at Phase 4. This document is the honest scope refinement: Wave 1 + Wave 2 lands here, Wave 3 is a documented backlog handed back for the client's ownership. If the client wants any Wave 3 item pulled into this engagement, we return to Phase 2 and re-plan; that request is welcome.
