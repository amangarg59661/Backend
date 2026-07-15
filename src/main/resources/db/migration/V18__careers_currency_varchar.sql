-- ============================================================================
-- V18__careers_currency_varchar.sql
-- Reconcile careers.job_postings.currency type with the JobPosting entity.
--
-- V17 declared the column as CHAR(3) but the JPA mapping is a plain
-- `private String currency;`, which Hibernate resolves to VARCHAR(255).
-- Under `spring.jpa.hibernate.ddl-auto=validate` the SessionFactory refuses
-- to boot with:
--   Schema-validation: wrong column type encountered in column [currency]
--   in table [careers.job_postings]; found [bpchar (Types#CHAR)], but
--   expecting [varchar(255) (Types#VARCHAR)].
--
-- ALTERing to VARCHAR(255) (no length pin because the entity does not pin
-- one either) matches Hibernate's default and keeps the pad-free semantics
-- so ISO 4217 codes stored earlier don't retain trailing spaces from CHAR.
-- ============================================================================

ALTER TABLE careers.job_postings
    ALTER COLUMN currency TYPE VARCHAR(255)
    USING TRIM(currency);
