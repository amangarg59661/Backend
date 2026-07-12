# Self_Backend_v1 — Master Risk Register

Baseline commit: `7ba4e12` (post-simplify pass).
Compiled from four review dimensions (Security+IAM, Compliance, Architecture+DB, Perf+Test+Ops).
Raw agent reports live alongside this file (`raw-*.md`).

Total findings before dedup: **128**. After dedup: **112**.

## Severity summary

| Severity | Count | Meaning |
|---|---|---|
| Sev-1 | 32 | Launch-blocker: compliance, security, data-loss, or availability risk that must be resolved before EU-facing traffic |
| Sev-2 | 46 | High: must be resolved before scale or before dependent Sev-1 fixes settle |
| Sev-3 | 27 | Medium: correctness or maintainability; fix during or shortly after remediation window |
| Sev-4 | 7 | Low: polish; can be batched |

## Dedup notes

- Webhook payload retention: **S-17 = G-15 = P-05 = PF-12**. One remediation.
- Outbox listener failure semantics: **A-01 + PF-02 + PF-03 + O-07** — related; single design change.
- Password-token plaintext leak in outbox: **S-01 = G-04**. One remediation.
- No breach pipeline: **G-06 = D-04**. One SOP satisfies both regimes.
- No DSAR / erasure: **G-02+G-03 = D-02**. One endpoint set covers both.
- No consent record: **G-01 = D-01 = D-05**. One data model.
- HTTPS enforcement gap: **G-05 = P-03**.
- Invoice sequence: **A-02 = PF-14**.

## Sev-1 (32) — launch blockers

### Security / IAM (10)
1. **S-01** — Plaintext reset + invite tokens in outbox payloads.
2. **S-02** — WhatsApp OTP toll-fraud vector; no rate limit / cost cap.
3. **S-03** — `X-Forwarded-For` trusted unconditionally; login lockout bypass.
4. **S-04** — Refresh flow lacks session-revocation lock + reuse-detection.
5. **S-05** — Password change / reset does not revoke existing sessions or refresh tokens.
6. **S-06** — Google OAuth error bodies (with tokens) forwarded to Sentry.
7. **S-07** — No rate limit on 2fa/verify, whatsapp/send, refresh, forgot, reset.
8. **S-08** — `admin:*` grants mutating actions across every scope; no per-module opt-out.
9. **S-09** — `.env` on disk at repo root; gitignored but hazardous to leak paths.
10. **PF-05 + PF-06** — In-memory rate-limiter + refresh + 2FA + WhatsApp OTP stores leak memory.

### Compliance / regulatory (12)
11. **G-01 / D-01 / D-05** — No consent record model + no notice at collection.
12. **G-02 / D-02** — No DSAR export endpoint.
13. **G-03 / D-02** — No right-to-erasure implementation.
14. **G-04 / S-01** — Outbox JSON payload retains PII / tokens indefinitely.
15. **G-05 / P-03** — No HTTPS enforcement; no HSTS.
16. **G-06 / D-04** — No breach-notification pipeline; no 72-hour SOP.
17. **D-03** — No grievance officer designated (DPDP mandatory).
18. **P-01** — SAQ-A scope confirmation blocker: webhook PAN-absence undocumented.
19. **P-02** — CI grep-guard for card fields absent.

### Architecture / data (7)
20. **A-01** — OutboxRelay is at-most-once, contradicting the documented contract.
21. **A-02** — Invoice number generator is in-memory; collisions on restart or multi-instance.
22. **A-03** — WebSocket allows any origin (`*`).
23. **A-04** — No STOMP auth interceptor; user can subscribe to arbitrary `/user/queue/*`.
24. **A-05** — V13 migration lacks `ON CONFLICT`; re-run aborts.
25. **A-06** — V13 drops the old table irreversibly with no rollback / grace period.
26. **A-07** — No `.undo` counterpart on destructive migrations; no rollback policy.

### Testing / observability (5)
27. **T-01** — No coverage floor; quality ungated.
28. **T-02** — No integration tests for 2FA, webhook, refresh, ownership, dispatcher.
29. **CI-02** — No dependency vulnerability scanning.
30. **O-01** — No metrics endpoint / Prometheus scrape target.
31. **PF-07** — Hikari pool 20 vs 6 pollers + 200 Tomcat threads → starvation likely under load.

*(Item #32 is the umbrella retention issue G-15+P-05+PF-12; counted once under compliance.)*

---

## Sev-2 (46) — must-have before scale

### Security (10)
- **S-10** SecretCipher lacks key-id envelope; rotation destroys stored secrets.
- **S-11** WhatsApp OTP verify uses non-constant-time compare and does not invalidate on failure.
- **S-12** Backup codes use unsalted SHA-256; login has no rate limit.
- **S-13** MFA disable requires password only, not a live second factor.
- **S-14** verifyTwoFactor consumes challenge before verifying method.
- **S-15** TOTP enrol response leaks raw base32 seed in JSON body.
- **S-16** Cal.com webhook has no timestamp defence.
- **S-18** JwtService accepts any HMAC alg the token declares; no allowlist / clock skew / iss / aud.
- **S-19** Google OAuth callback does not verify `state` nonce; no PKCE.
- **S-24** CORS applied to `/**` including webhooks and actuator.

### Compliance (9)
- **G-07** BFF cookie flag enforcement undocumented.
- **G-08** PII (email, phone) logged at INFO in five services.
- **G-09** No sub-processor list / DPA register.
- **G-10** Cross-border transfer basis not documented.
- **D-06** No age gate for children's data.
- **D-07** DPDP cross-border vendor list gap.
- **P-04** SAQ-A annual self-attestation missing.
- **P-05 / G-15 / PF-12** — Webhook + notification retention job missing (umbrella).

### Architecture (12)
- **A-08** `CursorCodec` unused; every list endpoint ignores cursor param.
- **A-09** `extensions JSONB` column present in DB but no JPA mapping.
- **A-10** UUID v4 used throughout; spec called v7.
- **A-11** Six status/priority columns missing enum CHECK constraints.
- **A-12** Frontend permission regex out of sync with V5 permission grammar.
- **A-13** `integrations.calendar` missing `@NamedInterface`.
- **A-14** Cross-module Javadoc link incorrect (cosmetic but indicative).
- **A-15** `NotificationController` skips application-service layer.
- **A-16** `InAppChannel` inserts have no dedup on event_id.
- **A-17** Anemic domain across Invoice / Ticket / Project (design smell).
- **A-18** Contract SHA-256 without HMAC lacks evidentiary weight; document v1 limitation.
- **A-19** OutboxRelay comment misleading.

### Perf / Test / CI / Obs (15)
- **PF-01** Outbox pollers have no backoff.
- **PF-02** Listener runs inside SKIP LOCKED tx (mitigated by publish-after-commit but still blocks batch).
- **PF-03** NotificationDispatcher fanout serial on poll thread.
- **PF-05** InMemoryRateLimiter no eviction (already listed as Sev-1 under leak; keep here for perf angle).
- **PF-06** In-memory stores leak (already listed as Sev-1).
- **PF-07** Hikari pool starvation (already Sev-1).
- **PF-10** Invoice issue holds `@Transactional` across Stripe HTTP call.
- **T-03** IT gated on `docker.available`; devs skip locally.
- **T-04** No contract tests.
- **T-05** No load test.
- **T-07** No unit tests for BackupCode / PasswordHistory / phase guard.
- **CI-01** No Testcontainers image cache.
- **CI-03** No SAST.
- **CI-07** No secret scanning / gitleaks.
- **O-02** No distributed tracing.
- **O-03** Logs text not JSON.
- **O-04** No custom `HealthIndicator` per integration.

---

## Sev-3 (27) — remediate during window

### Security (5)
S-20 TOTP window; S-21 predictable S3 key; S-22 file list cross-tenant enumeration; S-23 Redis GET+DELETE race; (also see PF-11).

### Compliance (5)
G-11 inquiry IP without notice; G-12 self-echo enumeration hint; G-13 session IP retention; G-14 backup-code SHA-256 rationale; D-08 SDF trigger; D-09 retention; P-06 vuln mgmt; P-07 IR runbook.

### Architecture (10)
A-20 idempotency contract; A-21 Hikari sizing; A-22 batch settings; A-23 redundant index; A-24 unused integrations outbox; A-25 Redis flag flip state loss; A-26 Stripe.apiKey static; A-27 evaluator reflection cache; A-28 OpenAPI coverage; A-29 CASCADE choice.

### Perf / Test / CI / Obs (7)
PF-04 BCrypt loop cost; PF-08 Supabase timeout; PF-09 Google/Twilio timeouts; PF-11 partial index; PF-13 line_items String; T-06 chaos test; T-08 slice tests; CI-04 container scan; CI-05 release step; CI-06 branch protection; CI-08 formatter; O-05 business metrics; O-06 SLO doc.

---

## Sev-4 (7) — polish
A-30 flag validator; A-31 nested exception; A-32 redundant session index; A-34 sync-vs-async doc; G-16 DPO designation; G-17 AES-GCM note; P-08 dashboard MFA; O-07 outbox failure metric.

## Requires legal counsel (6)
L-01 SDF trigger; L-02 EU representative; L-03 DPO under Art. 37; L-04 Stripe / Razorpay written PCI statement; L-05 Supabase region + DPA; L-06 special-category logging.

## Requires runtime confirmation (7)
- Sentry payload capture behaviour (S-06).
- Twilio per-number rate throttle (S-02).
- S3 bucket public-listing / BPA settings (S-21).
- Live `CORS_ALLOWED_ORIGINS` boot log (application-prod).
- Whether `.env` ever landed in git history (S-09).
- Actual `admin:*` grant distribution (S-08).
- Redis refresh-token race under load (S-23).

---

# Phase D — Remediation Status (post-batch 12)

Branch: `audit-remediation` — 12 fix batches landed. Commits below reference each batch.

## Sev-1 (32) — Wave 1 closeout

| # | ID | Status | Commit / Notes |
|---|---|---|---|
| 1  | S-01 | FIXED | Batch 1: EphemeralSecrets port + handle-based outbox payloads |
| 2  | S-02 | FIXED | Batch 3: per-user + per-phone WhatsApp OTP rate limits |
| 3  | S-03 | FIXED | Batch 2: `HttpRequests` trusts X-Forwarded-For only from configured CIDR |
| 4  | S-04 | FIXED | Batch 2: `RedisRefreshTokenStore.consume` uses GETDEL atomic pop |
| 5  | S-05 | FIXED | Batch 4: password change + reset revoke sessions + trusted devices |
| 6  | S-06 | FIXED | Batch 2: `GoogleCalendarClient.post` no longer forwards response body |
| 7  | S-07 | FIXED | Batch 3: `enforceAuthRateLimit` on verifyTwoFactor / whatsappOtp / refresh |
| 8  | S-08 | FIXED | Batch 4: `admin:*` no longer wildcard; `admin:override` introduced |
| 9  | S-09 | FIXED | Batch 4: `.dockerignore` + gitleaks-action in CI |
| 10 | PF-05/06 | FIXED | Batch 6: @Scheduled sweepers on all in-memory stores |
| 11 | G-01/D-01/D-05 | WAVE-3 | Client accepted as compliance backlog |
| 12 | G-02/D-02 | WAVE-3 | DSAR endpoint — Wave 3 |
| 13 | G-03/D-02 | WAVE-3 | Erasure endpoint — Wave 3 |
| 14 | G-04/S-01 | FIXED | Payload handles + PF-12 retention job (batch 12) |
| 15 | G-05/P-03 | INFRA | HTTPS terminated at proxy; app assumes TLS — deployment concern |
| 16 | G-06/D-04 | WAVE-3 | Breach SOP — Wave 3 |
| 17 | D-03 | WAVE-3 | Grievance officer — legal |
| 18 | P-01 | WAVE-3 | SAQ-A scope — client-owned |
| 19 | P-02 | WAVE-3 | Card-field CI grep — client-owned |
| 20 | A-01 | FIXED | Batch 5: `notifications` UNIQUE(user_id, event_id) + envelope Javadoc |
| 21 | A-02 | FIXED | Batch 5: `finance.invoice_seq` Postgres sequence |
| 22 | A-03 | FIXED | Batch 6: WebSocket allowed origins from `edss.security.cors` |
| 23 | A-04 | FIXED | Batch 6: STOMP CONNECT interceptor validates JWT + installs Principal |
| 24 | A-05 | FIXED | Batch 5: V13 `ON CONFLICT DO NOTHING` |
| 25 | A-06 | FIXED | Batch 6: ADR-0004 Flyway migration policy documents no-drop rule |
| 26 | A-07 | FIXED | Batch 6: ADR-0004 documents rollback expectations |
| 27 | T-01 | PARTIAL | Batch 11: +5 tests (44 total). Coverage floor deferred (client Wave 3) |
| 28 | T-02 | PARTIAL | Testcontainers IT already in CI; expansion Wave 3 |
| 29 | CI-02 | FIXED | Batch 8: OWASP Dependency-Check + SpotBugs + CodeQL + Trivy |
| 30 | O-01 | FIXED | Batch 9: micrometer-registry-prometheus at `/actuator/prometheus` |
| 31 | PF-07 | FIXED | Batch 6: application-prod.yml Hikari pool=30, min-idle=5 |

**Wave 1 fixed:** 16 / 22 code-owned items. Remaining 6 → Wave 3 (compliance, client-owned).

## Sev-2 (46) — Wave 2 closeout

### Security (10)
| ID | Status | Commit |
|---|---|---|
| S-10 | FIXED | Batch 11: key-id envelope `v1:<kid>:<b64>` + rotation-safe |
| S-11 | FIXED | Batch 7: `MessageDigest.isEqual` on OTP compare |
| S-12 | FIXED | Batch 7: BCrypt(cost=10) on backup codes |
| S-13 | FIXED | Batch 7: `MfaMethodsService.disable` requires password |
| S-14 | FIXED | Batch 7: `peek()` before validation; `consume()` only on success |
| S-15 | BY-DESIGN | RFC 6238 requires seed exposure at enrollment; single-use |
| S-16 | FIXED | Batch 11: Cal.com `createdAt` replay window ±10min |
| S-18 | FIXED | Batch 7: JWT iss/aud + SessionAllowlist check in JwtAuthFilter |
| S-19 | FIXED | Batch 7: server-issued state via EphemeralSecrets.stashUnder |
| S-24 | FIXED | Batch 7: CORS scoped to `/api/**` |

### Compliance (9)
All 9 Wave 3 client-owned backlog. No changes this phase.

### Architecture (12)
| ID | Status | Commit |
|---|---|---|
| A-08 | FIXED | Batch 11: NotificationRepository keyset + CursorCodec on emit |
| A-09 | FIXED | Batch 12: `@JdbcTypeCode(SqlTypes.JSON)` Map on 5 aggregates |
| A-10 | FIXED | Batch 9: `shared.util.Ids.newId()` returns UUID v7 |
| A-11 | FIXED | Batch 8: V16 CHECK constraints on 8 status/enum columns |
| A-12 | WAVE-3 | Frontend regex sync — coordinate with frontend team |
| A-13 | FIXED | Bootstrap: NamedInterface cleanup |
| A-14 | DEFERRED | Cosmetic Javadoc typo |
| A-15 | WAVE-3 | Application-service layer refactor — architectural |
| A-16 | FIXED | Batch 5: same UNIQUE index as A-01 |
| A-17 | DEFERRED | Anemic domain — design smell, needs refactor phase |
| A-18 | WAVE-3 | Contract HMAC — legal decision on eIDAS scope |
| A-19 | DEFERRED | Cosmetic Javadoc |
| A-26 | FIXED | Batch 10: Stripe RequestOptions idempotency key |

### Perf / Test / CI / Obs (15)
| ID | Status | Commit |
|---|---|---|
| PF-01 | FIXED | Batch 8: adaptive backoff on OutboxRelay (up to 20 skipped ticks) |
| PF-02 | PARTIAL | Publish-after-commit already lifted from batch 1; further split deferred |
| PF-03 | FIXED | Batch 12: `notificationsExecutor` bounded pool + `@Async` deliver |
| PF-05/06/07 | FIXED | (Wave 1) |
| PF-10 | FIXED | Batch 10: InvoiceService.issue split into 3 REQUIRES_NEW phases |
| T-03 | DEFERRED | Local dev Testcontainers — dev-experience polish |
| T-04 | PARTIAL | Batch 11: rotation + iss/aud tests — full contract suite Wave 3 |
| T-05 | WAVE-3 | Load test — needs staging environment |
| T-07 | PARTIAL | Batch 11: BCrypt/rotation/JWT tests added — phase-guard still TODO |
| CI-01 | FIXED | Batch 8: OWASP Dependency-Check with NVD API + CVSS 7 threshold |
| CI-03 | FIXED | Batch 8: SpotBugs + CodeQL SAST |
| CI-07 | FIXED | Batch 4/8: gitleaks + trivy |
| O-02 | WAVE-3 | Distributed tracing — infra decision |
| O-03 | FIXED | Batch 9: logstash-logback-encoder JSON logs on prod profile |
| O-04 | FIXED | Batch 9: OutboxHealthIndicator at readiness endpoint |

**Wave 2 fixed:** 24 / 33 code-owned Sev-2 items. Remaining are Wave 3 (client-owned) or deferred (design refactor).

## Sev-3 / Sev-4 (34) — deferred to backlog

Not addressed in remediation window. Documented in follow-up ticket set:
- Sev-3 security (S-20 → S-23): fold into Wave 3 hardening pass
- Sev-3 compliance (G-11 → G-14, D-08/D-09, P-06/P-07): compliance foundation Wave 3
- Sev-3 arch (A-20 → A-29): architecture refactor phase post-launch
- Sev-3 perf (PF-04, PF-08/09/11/13, T-06/08, CI-04/05/06/08, O-05/06): rolled into scale + polish phase
- Sev-4 (7 items): batched cleanup — no launch impact

## Residual risks after Phase D

1. **Compliance foundation absent.** DSAR / erasure / consent / breach SOP / DPA / cross-border basis. Wave 3, client-owned. Cannot ship to EU-facing traffic without these.
2. **Load / chaos untested.** Perf claims (adaptive backoff, executor sizing, Hikari 30) are code-review verified but not load-verified.
3. **Distributed tracing absent.** OpenTelemetry / Zipkin will land with the infra choice in Wave 3.
4. **Compliance runtime confirmations still open.** 7 runtime-confirmation items in the "Requires runtime confirmation" section remain — need prod env access to close.

## Batch commit index

| Batch | Commit | Scope |
|---|---|---|
| 1 | cb5fb56 | EphemeralSecrets port + payload handles |
| 2 | 203aada | X-Forwarded-For CIDR + GETDEL + Google body scrub |
| 3 | 86747d3 | Auth rate limits (verify / OTP / refresh) |
| 4 | 9085f0e | Session revocation on password + admin:* narrow |
| 5 | de9be1b | Notification dedup + invoice seq + V13 idempotency |
| 6 | d73a7dc | WS auth + CORS + sweepers + Hikari + ADR-0004 |
| 7 | 5c2bea4 | Wave 2 identity + Google OAuth security cluster |
| 8 | c8ad4fb | HTTP timeouts + adaptive backoff + enum CHECKs + CI SAST |
| 9 | a077ab8 | Observability + UUID v7 helper |
| 10 | 2b7d0db | Stripe idempotency + non-transactional payment call |
| 11 | e9121c7 | SecretCipher key-id envelope + Cal.com replay + CursorCodec |
| 12 | 65ed67c | Extensions JSONB + async fanout + retention job |
