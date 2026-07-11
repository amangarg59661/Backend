# Raw report — Architecture + DB + Modularity

## Sev-1 (7)
- **A-01** OutboxRelay marks published before publish → at-most-once. Silent event loss on async listener failure.
- **A-02** Invoice sequence is in-memory `AtomicLong`; resets across restarts + diverges across instances.
- **A-03** WebSocket allows `*` origin patterns; ignores CORS allowlist.
- **A-04** No WebSocket auth interceptor; any authenticated user can subscribe to any `/user/queue/*` route.
- **A-05** V13 `INSERT ... SELECT` has no ON CONFLICT; migration re-run aborts.
- **A-06** V13 `DROP TABLE identity.user_two_factor` is irreversible; no rollback / grace period.
- **A-07** No `.undo` counterparts on destructive migrations; no rollback policy.

## Sev-2 (12)
- **A-08** `CursorCodec` unused; every list endpoint accepts + ignores `cursor` param.
- **A-09** `extensions JSONB` promised on every aggregate — no JPA mapping exists.
- **A-10** UUID v4 across the board; spec called for v7. `uuid-creator` dep declared but unused.
- **A-11** Enum CHECK constraints missing on ~6 status/priority columns.
- **A-12** Frontend `permissionSchema` regex rejects `:own` suffix + 3-token grants that V5 loosened.
- **A-13** `integrations.calendar` submodule not `@NamedInterface`-tagged.
- **A-14** `CalendarWebhookService` Javadoc reaches into `projects.domain.OnboardingCall` — link only, but incorrect.
- **A-15** `NotificationController` depends directly on repo; no application-service seam.
- **A-16** `InAppChannel` has no dedup on event_id; Modulith event registry replay could duplicate rows.
- **A-17** Anemic domain across Invoice / Ticket / Project — logic in services.
- **A-18** Contract SHA-256 alone insufficient legal evidence for signatures; document v1 limitation.
- **A-19** OutboxRelay comment misleading.

## Sev-3 (12)
- **A-20** No idempotency contract documented for listeners.
- **A-21** Hikari pool 20 across profiles — starves under listener latency.
- **A-22** Hibernate batch settings unset.
- **A-23** V11 `ix_notifications_user_read` overlaps V1's `ix_notifications_user_created`.
- **A-24** V12 creates `integrations.outbox` but no relay exists.
- **A-25** State loss when Redis flag flips mid-flight; in-memory stores empty on switch.
- **A-26** StripeGateway sets `Stripe.apiKey` JVM-global from constructor.
- **A-27** OwnershipPermissionEvaluator does reflection lookup per call; no method cache.
- **A-28** OpenAPI coverage near zero: 0-1 `@Operation` per controller.
- **A-29** Projects tables use ON DELETE CASCADE; silent history loss on hard delete.

## Sev-4 (4)
- **A-30** No startup validator for unknown `edss.features.*` keys.
- **A-31** `EmailAlreadyExistsException` nested inside interface; awkward to catch.
- **A-32** V3's `ix_identity_sessions_active` supersedes V1's `ix_identity_sessions_user`.
- **A-33** No `@Query` hints (bright spot — indexes correct).
- **A-34** Sync-vs-async listener decision undocumented in class Javadoc.
