-- ============================================================================
-- V19__finance_currency_varchar.sql
-- Reconcile finance.invoices.currency type with the Invoice entity.
--
-- Same shape as V18 — V1 declared this column as CHAR(3) NOT NULL but the
-- JPA mapping is a plain String, which Hibernate resolves to VARCHAR(255).
-- Under `spring.jpa.hibernate.ddl-auto=validate` the SessionFactory refuses
-- to boot with:
--   Schema-validation: wrong column type encountered in column [currency]
--   in table [finance.invoices]; found [bpchar (Types#CHAR)], but
--   expecting [varchar(255) (Types#VARCHAR)].
--
-- V1 cannot be edited in place — its Flyway checksum is locked from every
-- previous successful run.  Do the fix here.  ISO 4217 codes stored while
-- the column was CHAR carry trailing pad spaces; TRIM strips them.
-- ============================================================================

ALTER TABLE finance.invoices
    ALTER COLUMN currency TYPE VARCHAR(255)
    USING TRIM(currency);
