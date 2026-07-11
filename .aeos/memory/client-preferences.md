# Client Preferences — Self_Backend_v1 Production-Readiness Audit

## Engagement summary

- **Client** — solo founder / product owner of Self_Backend_v1 backend.
- **Depth** — full production-readiness audit (all ten review stages).
- **Timing** — quality-first; no fixed launch deadline.
- **Compliance scope** — India DPDP Act 2023, GDPR, PCI-DSS SAQ-A.
- **Remediation scope** — every confirmed finding fixed inside this engagement.
- **Baseline** — commit `7ba4e12` on `main` (post-simplify pass).
- **Working branch** — `audit-remediation`.

## Commitments

Governance objects. Every one is tracked and reported in the executive report at close.

1. Every review stage checklist is answered. No silent skips.
2. Every confirmed finding is fixed inside this engagement.
3. Every fix passes the review pipeline before it lands on the working branch.
4. Material design decisions become ADRs in `.aeos/memory/adr/`.
5. The executive report closes the engagement with a residual-risk statement and a signed-off backlog.
6. If a finding cannot be remediated inside this engagement, it is declared explicitly, with reason, and moved to the client-owned backlog with the client's assent — not silently deferred.

## Working preferences observed so far

- Client keeps the codebase under `main`, uses conventional Co-Authored-By commits, no PR gate.
- Client prefers explicit, terse status updates outside of client-facing deliverables.
- Client prefers full remediation over report-only. Commitment #2 above reflects that.
- Client already ran `/simplify` before this audit — trivial reuse and cleanup fixes are already in `main`.
