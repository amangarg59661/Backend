-- ============================================================================
-- V20__projects_currency_varchar.sql
-- Reconcile projects.projects.currency type with the Project entity.
--
-- V7__projects_lifecycle.sql added `currency CHAR(3)` via ALTER TABLE
-- (which my earlier CHAR sweep missed — it was scanning for CREATE TABLE
-- column definitions, not ALTER TABLE ADD COLUMN forms).  Same
-- Hibernate schema-validation failure as V18 (careers) and V19 (finance):
--   Schema-validation: wrong column type encountered in column [currency]
--   in table [projects.projects]; found [bpchar (Types#CHAR)], but
--   expecting [varchar(255) (Types#VARCHAR)].
--
-- ALTERs to VARCHAR(255) with TRIM(currency) so any ISO 4217 codes stored
-- while the column was CHAR do not retain pad-space.
-- ============================================================================

ALTER TABLE projects.projects
    ALTER COLUMN currency TYPE VARCHAR(255)
    USING TRIM(currency);
