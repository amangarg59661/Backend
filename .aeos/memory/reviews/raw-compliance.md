# Raw report — Compliance (GDPR + India DPDP + PCI-DSS SAQ-A)

## GDPR (17)
- **G-01 Sev-1** No consent record model. No `consent`, `gdpr`, `lawful_basis` in `src/main`.
- **G-02 Sev-1** No DSAR export endpoint; no `/me/export` or portability endpoint.
- **G-03 Sev-1** No right-to-erasure implementation across identity/relationship/commitments/finance.
- **G-04 Sev-1** Inquiry PII forwarded verbatim into outbox JSON payloads that never purge. Same in password reset + invite.
- **G-05 Sev-1** No HTTPS enforcement in Spring layer; no HSTS; relies solely on undocumented upstream terminator.
- **G-06 Sev-1** No breach-notification pipeline; no 72-hour SOP.
- **G-07 Sev-2** Refresh-token cookie flag enforcement not documented backend-side; BFF assumed.
- **G-08 Sev-2** PII (email, phone) logged at INFO across `EmailChannel`, `WhatsappChannel`, `WhatsappOtpService`, `AuthService`, `AdminSeedRunner`.
- **G-09 Sev-2** No sub-processor list / DPA register (`docs/subprocessors.md` missing).
- **G-10 Sev-2** Cross-border transfer basis not documented (Stripe US, Sentry, Resend, Twilio, Supabase, AWS S3).
- **G-11 Sev-3** Public inquiry endpoint captures IP without notice / policy version acceptance.
- **G-12 Sev-3** DTO on submit echoes internal id UUID — enumeration hint.
- **G-13 Sev-3** Sessions + trusted-device IP/user_agent retained indefinitely beyond session expiry.
- **G-14 Sev-3** Backup codes SHA-256 unsalted at rest — acceptable given high entropy; document rationale.
- **G-15 Sev-3** Payment webhook payload retention indefinite.
- **G-16 Sev-4** No DPO / EU representative designated in code / config.
- **G-17 Sev-4** AES-GCM at rest for TOTP + Google tokens confirmed compliant.

## India DPDP Act 2023 (9)
- **D-01 Sev-1** No notice mechanism at collection.
- **D-02 Sev-1** No data-principal rights endpoints.
- **D-03 Sev-1** No grievance officer / DPO designated.
- **D-04 Sev-1** No breach notification pipeline (DPB + affected principals).
- **D-05 Sev-2** No consent-manager integration hook.
- **D-06 Sev-2** No age gate for children's data.
- **D-07 Sev-2** Cross-border transfer vendor list missing.
- **D-08 Sev-3** Significant Data Fiduciary trigger review needed (legal counsel).
- **D-09 Sev-3** Retention limits — shared code gap with GDPR.

## PCI-DSS SAQ-A (8)
- **P-01 Sev-1** Webhook payload PAN-absence not documented; provisional SAQ-A eligibility.
- **P-02 Sev-1** No card fields in application forms — confirmed compliant; add CI grep guard.
- **P-03 Sev-2** TLS enforcement gap (see G-05).
- **P-04 Sev-2** Annual SAQ-A self-attestation missing.
- **P-05 Sev-2** Webhook payload retention indefinite (PCI angle).
- **P-06 Sev-3** Vulnerability management (SAQ-A v4.0 anti-skimming) — no dependency scan.
- **P-07 Sev-3** No incident-response runbook or security-awareness training log.
- **P-08 Sev-4** Stripe/Razorpay dashboard access — operational MFA + logging.

## Requires legal counsel
- L-01 Significant Data Fiduciary trigger (DPDP §10)
- L-02 EU representative under GDPR Art. 27
- L-03 DPO designation Art. 37
- L-04 Written statement from Stripe & Razorpay on webhook payload content re PCI CHD
- L-05 Supabase region + DPA + SCCs
- L-06 Special-category data risk in logging
