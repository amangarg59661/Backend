# Deployment — Render (today) → VPS / AWS (future)

## Runtime shape

`self-backend` is a Spring Boot 3.2 modular monolith. Runs as one process,
binds to `$SERVER_PORT` (default 8080), reads config from environment
variables, connects to an external Supabase Postgres, and (optionally) a
Redis instance for distributed rate-limits + refresh-token store + session
sharing.

Actuator surfaces at `/actuator/health`, `/actuator/health/liveness`,
`/actuator/health/readiness`, `/actuator/prometheus`, `/actuator/metrics`.
Never expose these publicly — a reverse proxy or platform ingress must
whitelist `/actuator/*` for the health checker only.

## Provider matrix

| Concern     | Provider       | Provisioned by      |
| ----------- | -------------- | ------------------- |
| Compute     | Render         | `render.yaml`       |
| Registry    | GHCR           | `release.yml`       |
| Postgres    | Supabase       | Supabase dashboard  |
| Redis       | Render add-on  | `render.yaml`       |
| Mail        | Resend         | resend.com          |
| Observ.     | Sentry         | sentry.io           |
| Payments    | Stripe + Razorpay | respective dashboards |
| WhatsApp    | Twilio         | twilio.com          |
| Calendar    | Cal.com        | cal.com             |
| Storage     | Supabase Storage | Supabase dashboard |

## First-time Render setup

1. Push this repo to GitHub if not already.
2. Render dashboard → **New +** → **Blueprint** → point at the repo, branch
   `main`. Render reads `render.yaml`, creates:
   - `self-backend` (web service, Docker runtime, plan `starter`).
   - `self-backend-redis` (Redis add-on, plan `starter`).
3. On the first apply Render prompts for every env var marked `sync: false`
   in `render.yaml`. Fill from the checklist below. `generateValue: true`
   values (`JWT_SECRET`, `SECRET_ENCRYPTION_KEY`) are auto-generated —
   do not paste your own here.
4. Trigger a deploy. First cold start takes ~2 min (Maven cache miss +
   Flyway migrations against Supabase).
5. Once healthy, verify `GET /actuator/health/readiness` returns 200 via
   the Render "Shell" tab: `curl -sS localhost:$PORT/actuator/health`.

## Secret checklist — `sync: false` values to paste in Render

Copy from `.env.production.example` after replacing every `<placeholder>`
with the real value. Never commit filled versions.

- Database — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Resend — `RESEND_API_KEY`, `MAIL_FROM`
- Sentry — `SENTRY_DSN`
- Supabase Storage — `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`
- Stripe — `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`
- Razorpay — `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`,
  `RAZORPAY_WEBHOOK_SECRET`
- Twilio — `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`,
  `TWILIO_WHATSAPP_FROM`
- Cal.com — `CALCOM_API_KEY`, `CALCOM_WEBHOOK_SECRET`
- Google OAuth — `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`,
  `GOOGLE_OAUTH_REDIRECT_URI`
- CORS — `CORS_ALLOWED_ORIGINS` (comma-separated, no trailing slash,
  no wildcards)

Rotate `JWT_SECRET` and `SECRET_ENCRYPTION_KEY` requires a coordinated
downtime — rotating either invalidates every issued token / stored TOTP
seed.

## Release flow

`release.yml` builds and pushes the production image to GHCR after CI
passes on `main`:

- Tag: `ghcr.io/<owner-lc>/self-backend:<sha>`, `…:<short>`, `…:main`,
  `…:latest`.
- BuildKit cache is stored in GitHub Actions cache so subsequent pushes
  reuse Maven and Docker layer state.

Render deploys directly from the repo Dockerfile via `render.yaml`'s
`runtime: docker`, so it does not consume the GHCR image today. The image
is published anyway so a later migration to a VPS or AWS ECS is a
`docker pull` away.

## Migration path — VPS

When traffic outgrows Render (or when the monthly cost crosses the price
of a Hetzner CX21):

1. Provision the VPS with Docker + docker compose + a reverse proxy
   (Caddy or Traefik) that terminates TLS via Let's Encrypt.
2. Copy the env-var values from Render into a systemd EnvironmentFile or
   a Docker secret; do not commit.
3. `docker pull ghcr.io/<owner-lc>/self-backend:<sha>` and run under the
   reverse proxy. Health check target: `/actuator/health/readiness`.
4. Keep the Supabase Postgres and Resend and Sentry — only the compute
   layer moves.
5. Cut DNS over after a green health check on the new instance for at
   least 24 h.

## Migration path — AWS

For AWS ECS on Fargate:

1. Push the same GHCR image into ECR — either mirror via `docker pull` +
   `docker push`, or point a Copilot / CDK task at the GHCR URL directly
   (ECS accepts external registries with proper IAM).
2. Store every `sync: false` secret in AWS Secrets Manager, wire into
   the task definition.
3. Front with an ALB → target group on port 8080, health check path
   `/actuator/health/readiness`.
4. RDS Aurora Postgres (or keep Supabase) — application config only
   needs `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` swapped.
5. ElastiCache Redis replaces the Render add-on. Point `REDIS_HOST` etc.
   at the new endpoint.

The Dockerfile does not change between these targets.

## Local docker compose

`docker-compose.yml` at the repo root brings up Postgres 14, Mailhog, and
optionally Redis (profile `scale`). Not intended for prod.

```bash
docker compose up -d postgres mailhog
cp .env.development.example .env
mvn spring-boot:run
```
