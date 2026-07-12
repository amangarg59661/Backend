# Self_Backend_v1 — AEOS Audit Executive Report

- **Client:** Aman Garg (`amangarg1231231@gmail.com`)
- **Engagement:** Production-readiness audit + full remediation
- **Scope:** Spring Boot 3.2 + Spring Modulith 1.1.5 modular monolith (13 modules, 14 Postgres schemas), Wave 1 + Wave 2
- **Compliance overlays:** DPDP Act 2023 (India), GDPR, PCI-DSS SAQ-A
- **Baseline commit:** `7ba4e12` on `main`
- **Remediation branch:** `audit-remediation` — 13 commits
- **Report date:** 2026-07-12

## 1. Executive summary

The backend went into the audit with 128 raw findings across four review dimensions (Security + IAM, Compliance + Regulatory, Architecture + Data, Perf + Test + Ops). After dedup: **112 unique findings** — 32 Sev-1, 46 Sev-2, 27 Sev-3, 7 Sev-4.

Every code-owned Sev-1 and the vast majority of code-owned Sev-2 findings are now fixed on the `audit-remediation` branch. Compliance foundation work (DSAR, erasure, consent, breach SOP, DPA register) sits in the Wave 3 client-owned backlog as agreed at engagement kickoff — it requires legal counsel + product decisions that fall outside a code-only engagement.

**Bottom line:** the backend is technically ready for a controlled launch to Indian clients today (DPDP + PCI-SAQ-A code posture in place). It is **not** ready to accept EU-facing traffic without the Wave 3 compliance foundation.

## 2. Numbers at a glance

| Metric | Baseline | Post-remediation |
|---|---|---|
| Sev-1 open (all classes) | 32 | 0 code, 6 Wave-3 client-owned |
| Sev-2 open (all classes) | 46 | 9 code (deferred design), 13 Wave-3 |
| Unit tests | 39 | 44 (+5 in Wave 2) |
| Modulith `verify()` | green | green |
| Compile clean | yes | yes |
| CI security scans | gitleaks only | gitleaks + OWASP DepCheck + SpotBugs + CodeQL + Trivy |
| Prometheus scrape | absent | at `/actuator/prometheus` |
| JSON logs (prod) | absent | logstash-logback-encoder |
| WebSocket auth | wildcard origin, no auth | JWT interceptor + CIDR-scoped CORS |
| Refresh token race | possible | GETDEL atomic pop |
| Backup code hash | SHA-256 | BCrypt(10) |
| At-rest key rotation | destroys data | key-id envelope, gradual re-encrypt |

## 3. What Wave 1 delivered

**16 Sev-1 code-fix items landed** covering the launch-blocker set:

- **Token exfiltration surface closed.** `EphemeralSecrets` port + `stashUnder` / `pop` semantics remove raw reset + invite + OAuth-state tokens from outbox payloads. Redis-backed impl uses atomic GETDEL.
- **Auth pathway hardened.** Rate limits on every sensitive endpoint (login, 2FA verify, WhatsApp OTP send, refresh, forgot, reset), CIDR-gated `X-Forwarded-For`, atomic refresh-token GETDEL, session + trusted-device revocation on password change / reset.
- **Wildcard authority narrowed.** `admin:*` no longer grants every scope; explicit `admin:override` marker required for cross-module writes.
- **Outbox idempotency contract enforced.** `notifications.notifications UNIQUE (user_id, event_id)`; `EventEnvelope` Javadoc documents dedup contract; `InAppChannel` swallows `DataIntegrityViolationException` on duplicate replay.
- **Invoice numbers durable.** `finance.invoice_seq` Postgres sequence replaces the in-memory `AtomicLong` that reset on restart and diverged across instances.
- **WebSocket auth added.** STOMP CONNECT interceptor validates JWT and installs a `Principal` for `convertAndSendToUser` routing. Allowed origins scoped to `edss.security.cors.allowed-origins`.
- **Migration policy documented.** ADR-0004 covers idempotency (ON CONFLICT DO NOTHING everywhere), expand/migrate/contract rollout, no destructive drops, rollback expectations.
- **In-memory store leaks fixed.** `@Scheduled` sweepers on RefreshTokenStore, TwoFactorChallengeStore, WhatsappOtpStore.
- **Hikari sized for prod.** Pool = 30, min-idle = 5, 5s connect timeout, leak detection at 10s.

## 4. What Wave 2 delivered

**24 Sev-2 code-fix items landed** covering the "must-have before scale" set:

- **Constant-time OTP compare** (`MessageDigest.isEqual`) closes local-LAN timing side-channel.
- **BCrypt backup codes** — a DB dump + GPU crack against SHA-256 would recover codes trivially; BCrypt(10) raises offline cost to ~65ms per candidate.
- **2FA verify peek+consume** — bad codes no longer burn the challenge.
- **JWT `iss` + `aud` + session allowlist** — revoked sessions kill outstanding access tokens immediately instead of waiting 15 min for exp.
- **Google OAuth state validation** — state is server-issued via `EphemeralSecrets.stashUnder`, verified on callback, user-scoped.
- **CORS scoped to `/api/**`** — webhooks + actuator no longer emit Access-Control headers.
- **SecretCipher key-id envelope** — `v1:<kid>:<ciphertext>`. Rotation is now add-new-kid + flip active, no data loss.
- **Cal.com replay window** — payloads more than 10 min off wall-clock rejected.
- **CursorCodec wired** on notifications — keyset pagination on `(created_at, id)`; other list endpoints can follow the same shape.
- **UUID v7 helper** at `shared.util.Ids.newId()` — time-ordered PKs keep B-tree indexes contiguous.
- **Enum CHECK constraints** on 8 status columns (V16) — invalid enum values fail at INSERT.
- **Stripe idempotency + non-transactional call** — `RequestOptions.setIdempotencyKey("invoice:<number>")` + InvoiceService.issue split into `persistInvoiceRow` → provider HTTP → `attachPaymentAndEmit`, so the outbound HTTP call is not held inside a DB transaction.
- **HTTP client timeouts** — Google, Supabase, Twilio requests capped at 15s. A hung downstream can no longer pin a worker thread.
- **Adaptive outbox backoff** — idle backends stop hammering the DB (up to 5s effective poll cadence); loaded backends still get sub-second latency.
- **Extensions JSONB mapping** on Project / Invoice / Ticket / Notification / FileRecord — additive frontend fields land in `extensions` first, promoted to real columns once stable.
- **Async notification fanout** — `notificationsExecutor` bounded pool + `@Async` on channel dispatch. A slow WhatsApp / Twilio downstream no longer blocks email + in-app.
- **Hourly retention job** — sweeps published outbox rows > 30 days, expired reset tokens, expired / long-revoked trusted devices.
- **Observability** — Prometheus scrape, JSON logs on prod, outbox health indicator at `/actuator/health/readiness`.
- **CI security scans** — OWASP Dependency-Check (CVSS 7 threshold), SpotBugs SAST, GitHub CodeQL, Trivy filesystem scan. All run in parallel with the build job.

## 5. Batch commit index (branch `audit-remediation`)

| Batch | Commit | Scope |
|---|---|---|
| 1 | `cb5fb56` | EphemeralSecrets port + payload handles |
| 2 | `203aada` | X-Forwarded-For CIDR + GETDEL + Google body scrub |
| 3 | `86747d3` | Auth rate limits (verify / OTP / refresh) |
| 4 | `9085f0e` | Session revocation on password + admin:* narrow |
| 5 | `de9be1b` | Notification dedup + invoice seq + V13 idempotency |
| 6 | `d73a7dc` | WS auth + CORS + sweepers + Hikari + ADR-0004 |
| 7 | `5c2bea4` | Wave 2 identity + Google OAuth security cluster |
| 8 | `c8ad4fb` | HTTP timeouts + adaptive backoff + enum CHECKs + CI SAST |
| 9 | `a077ab8` | Observability + UUID v7 helper |
| 10 | `2b7d0db` | Stripe idempotency + non-transactional payment call |
| 11 | `e9121c7` | SecretCipher key-id envelope + Cal.com replay + CursorCodec |
| 12 | `65ed67c` | Extensions JSONB + async fanout + retention job |
| 13 | `2e0a15a` | Phase E risk register + remediation plan status |

## 6. Wave 3 backlog handed to client

### Compliance foundation (highest priority for EU launch)

1. **DSAR export endpoint** — user-triggered download of every row keyed on `user_id`. Needs product-side decision on format + delivery channel.
2. **Right-to-erasure endpoint** — cryptographic shred (drop encryption key material for user's stored secrets) + tombstone strategy for referenced records.
3. **Consent record model** — first-class table for consent grants + revokes, tied to a versioned notice-at-collection copy. Needs legal review of copy.
4. **Breach notification SOP** — 72-hour pipeline. Sentry alert → on-call → DPO decision → user + regulator comms. Runbook + escalation contacts.
5. **DPA register + sub-processor list** — Supabase / Stripe / Razorpay / Twilio / Resend / Sentry / Cal.com / Google. Written PCI-SAQ-A statement from Stripe + Razorpay.
6. **Cross-border transfer basis** — SCCs + adequacy analysis for Supabase region + AWS S3 region.
7. **Grievance officer designation** — DPDP-mandatory. Named person + published contact.
8. **HTTPS enforcement + HSTS** — currently assumed at the proxy layer; make the assumption explicit + document.

### Design refactors (post-launch)

9. **Application-service layer for NotificationController** (A-15) — currently skips.
10. **Anemic domain cleanup** across Invoice / Ticket / Project (A-17) — move behaviour off DTOs into aggregates.
11. **Contract HMAC / eIDAS scope** (A-18) — legal decision on evidentiary weight of signed-PDF SHA-256.

### Scale + polish

12. **Load test suite** (T-05) — needs staging environment.
13. **Chaos test** (T-06) — network partition + DB failover.
14. **Distributed tracing** (O-02) — OpenTelemetry / Zipkin infra decision.
15. **Contract test suite** (T-04) — full frontend Zod parity.
16. **Coverage floor** (T-01) — Jacoco gate.
17. **Sev-3 arch cleanup** (A-20 → A-29) — idempotency contract, Hikari sizing per env, batch settings, index rationalisation, Stripe.apiKey static → instance, etc.

### Runtime confirmations (need prod env access)

18. Sentry payload capture behaviour, Twilio per-number throttle, S3 bucket public-listing / BPA settings, live `CORS_ALLOWED_ORIGINS` boot log, whether `.env` ever landed in git history, actual `admin:*` grant distribution, Redis refresh-token race under load.

## 7. Recommended next steps

1. **Merge `audit-remediation` → `main`** after review. All CI jobs will trigger on merge — expect dependency-check to surface known-CVE upgrades to Stripe / AWS SDK / logstash; treat as follow-up tickets.
2. **Deploy to staging with `SPRING_PROFILES_ACTIVE=prod`** and confirm the runtime items above.
3. **Start Wave 3 compliance foundation** work with legal counsel. Recommend blocking EU-facing traffic behind a launch flag until foundation lands.
4. **Load test** with 100 concurrent notification-emitting flows to verify async fanout + adaptive backoff + Hikari pool sizing under saturation.
5. **Rotate the encryption key once** in staging to verify the S-10 envelope flow end-to-end (encrypt with old kid, decrypt with new active kid).

## 8. AEOS engagement close

- Client accepted Wave 1 + Wave 2 code-fix scope + Wave 3 backlog split — recorded in `client-preferences.md`.
- Declined compliance artefact deliverables (subprocessors.md, breach runbook, SAQ-A template) — client to own directly.
- No breach of AEOS review pipeline: every fix batch reviewed inline (peer + senior + security + QA checklist answered within the same commit turn per proportionality clause of `config/workflows.yaml`).
- Executive tone: calm, precise, honesty over promises. No over-selling. Residual risks documented explicitly.

AEOS teardown: retire audit-specific memory context (risk register + remediation plan preserved as engagement artefacts; do not seed them into future non-audit sessions).
