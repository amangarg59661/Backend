# Raw report — Performance + Testing + CI + Observability

## Performance (14)
- **PF-01 Sev-2** 6 outbox pollers × 250ms = 24 tx/s idle baseline; no backoff.
- **PF-02 Sev-2** OutboxRelay listener runs inside SKIP LOCKED tx; slow listener blocks the batch.
- **PF-03 Sev-2** NotificationDispatcher serial fanout on poll thread.
- **PF-04 Sev-3** BCrypt(12) history loop of up to 4 hashes = ~1s per password change.
- **PF-05 Sev-2** InMemoryRateLimiter has no eviction of expired entries; memory grows.
- **PF-06 Sev-2** In-memory refresh + 2FA challenge + WhatsApp OTP stores share the same leak.
- **PF-07 Sev-2** Hikari pool=20 vs 6 continuous pollers + Tomcat 200 + @Async workers; starvation risk.
- **PF-08 Sev-3** Supabase presign HTTP has no per-request timeout.
- **PF-09 Sev-3** Google + Twilio HTTP calls sync inside listener path; block relay thread.
- **PF-10 Sev-2** InvoiceService.issue holds `@Transactional` across Stripe HTTP call.
- **PF-11 Sev-3** V11 `notifications` index not partial.
- **PF-12 Sev-2** `payment_webhook_events` + `calendar_webhook_events` grow unbounded.
- **PF-13 Sev-3** Invoice `line_items` stored as String; rehydrated per read.
- **PF-14 Sev-4** Invoice sequence resets on restart (dup of A-02).

## Testing (8)
- **T-01 Sev-1** No coverage floor; 36 unit tests unable to gate quality.
- **T-02 Sev-1** No integration tests for 2FA / payment webhook / presign / calendar webhook / outbox / dispatcher / refresh / ownership.
- **T-03 Sev-2** IT gated on `docker.available=true`; devs skip locally.
- **T-04 Sev-2** No contract tests vs frontend Zod schemas.
- **T-05 Sev-2** No load test.
- **T-06 Sev-3** No chaos test for outbox at-least-once.
- **T-07 Sev-2** No unit tests for BackupCode single-use, PasswordHistory reuse rejection, project-phase guard.
- **T-08 Sev-3** No per-module `@ApplicationModuleTest` slice.

## CI / DevOps (8)
- **CI-01 Sev-2** No Testcontainers image caching; cold pull each run.
- **CI-02 Sev-1** No dependency vulnerability scanning (Dependabot / OWASP dep-check).
- **CI-03 Sev-2** No SAST (SpotBugs / CodeQL).
- **CI-04 Sev-3** No container image, no Trivy scan.
- **CI-05 Sev-3** No release step / SemVer tag flow.
- **CI-06 Sev-3** Branch protection not encoded.
- **CI-07 Sev-2** No secret scanning / gitleaks.
- **CI-08 Sev-3** No formatter/lint gate.

## Observability (7)
- **O-01 Sev-1** No Prometheus / metrics endpoint exposed.
- **O-02 Sev-2** No distributed tracing.
- **O-03 Sev-2** Logs are text not JSON.
- **O-04 Sev-2** No custom `HealthIndicator` for Redis/Twilio/Stripe/Supabase.
- **O-05 Sev-3** No business metrics (invoice, 2FA, outbox lag).
- **O-06 Sev-3** No SLO / error-budget docs.
- **O-07 Sev-4** Outbox listener failure log-only; no Sentry capture + metric.
