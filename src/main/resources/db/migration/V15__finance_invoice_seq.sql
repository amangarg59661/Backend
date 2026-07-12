-- ============================================================================
-- V15__finance_invoice_seq.sql
-- DB-side sequence for invoice numbers. Replaces the in-memory AtomicLong
-- that reset across restarts and diverged across instances. Fixes A-02.
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS finance.invoice_seq
    START 1
    INCREMENT 1
    NO CYCLE;
