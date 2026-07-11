# Raw report — Security + Crypto + IAM review

Findings S-01..S-24 as returned by review agent. Sev-1 highest.

## Sev-1 (9)
- **S-01** Plaintext reset + invite tokens persisted in module outbox JSON payload. Source: `PasswordResetService.java:71-81`, `InquiryService.java:126-132`, `IdentityUserProvisioningService.java:87`. DB reader / backup / replica / Sentry payload capture can silently redeem accounts.
- **S-02** WhatsApp OTP send has no rate limit + no cost cap. `WhatsappOtpService.java:34-49`, `SelfServiceController.java:139-145`, `AuthController /2fa/whatsapp/send`. Toll fraud + Twilio account termination risk.
- **S-03** `HttpRequests.clientIp` trusts XFF unconditionally. Login rate-limit bypass by header rotation.
- **S-04** Refresh flow does not re-check `sessions.revoked_at` under a lock; no refresh-token family/lineage; stolen refresh remains valid alongside legit user. `AuthService.refresh:202-239`.
- **S-05** Password change + reset do not revoke existing sessions or refresh tokens. `PasswordChangeService.java:40-53`, `PasswordResetService.java:83-109`.
- **S-06** GlobalExceptionHandler forwards raw exception messages (including Google OAuth error bodies which may contain tokens) to Sentry. `GlobalExceptionHandler.java:59-71`.
- **S-07** No rate limit on `/auth/2fa/verify`, `/auth/2fa/whatsapp/send`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`. Only `/auth/login` is limited.
- **S-08** `admin:*` acts as super-wildcard for every scope including mutating actions. `OwnershipPermissionEvaluator.java:64-79`.
- **S-09** `.env` present on disk at repo root; risk of leaking via `COPY . .`, forgotten add, or CI artifact. Gitignored, but still hazardous.

## Sev-2 (10)
- **S-10** `SecretCipher` has no key-id envelope; rotating the key silently breaks every stored TOTP + Google refresh token.
- **S-11** `WhatsappOtpService.verify` uses `String.equals` (non-constant-time) and does not invalidate the OTP on failed attempts. 10⁶-code grind within 5-minute TTL.
- **S-12** `BackupCodeService.consume` uses unsalted SHA-256 on backup codes. Fine if DB never leaks; BCrypt safer. Also no rate limit on backup-code login.
- **S-13** `MfaMethodsService.disable` requires password only, not a second factor. Phished password strips 2FA.
- **S-14** `verifyTwoFactor` consumes the challenge before verifying the method — wrong method choice burns the challenge.
- **S-15** TOTP enrolment start response includes the raw base32 secret in JSON body; frontend logging leaks the seed.
- **S-16** Cal.com webhook has no timestamp defence; captured body can be replayed indefinitely.
- **S-17** Payment + calendar webhook raw payloads stored forever. Customer email + card fingerprint present in Stripe payloads.
- **S-18** JwtService accepts any HMAC alg the token declares; no explicit allowlist; no clockSkewSeconds; no iss/aud claim requirement.
- **S-19** Google OAuth callback does not validate `state` against a stored per-user nonce; no PKCE. Phished code can be exchanged by any authenticated user.

## Sev-3 (5)
- **S-20** TOTP verifier uses default ±1 step window; combined with missing rate-limit (S-07) enlarges brute-force multiplier.
- **S-21** Presigned S3 URLs use predictable object key (userId + UUID + name); combined with any bucket-listing misconfig, enumerable.
- **S-22** `FileService.list` returns files for any `projectId` without verifying ownership. Cross-tenant enumeration of file metadata.
- **S-23** `RedisRefreshTokenStore.consume` uses GET then DELETE (non-atomic); concurrent refresh with stolen token can both succeed.
- **S-24** CORS applied to `/**` including `/api/v1/webhooks/**` and `/actuator/**`. Should be `/api/v1/**` minus webhooks.
