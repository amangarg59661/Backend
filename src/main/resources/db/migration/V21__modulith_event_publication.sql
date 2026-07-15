-- ============================================================================
-- V21__modulith_event_publication.sql
-- Spring Modulith's JPA event-publication registry.
--
-- The `spring-modulith-events-jpa` starter ships a `JpaEventPublication`
-- @Entity that maps to a table named `event_publication`.  Unlike every
-- app entity in this codebase it does not declare `@Table(schema = "…")`,
-- so under `spring.jpa.hibernate.ddl-auto=validate` Hibernate looks it up
-- in the default schema and bails on boot with:
--   Schema-validation: missing table [event_publication].
--
-- The starter has no schema-init resource of its own for the JPA variant
-- (only the JDBC variant carries schema-postgresql.sql), so the DDL has
-- to be part of the app's own Flyway history.  Column shapes below match
-- Modulith 1.1.5's `JpaEventPublication` field-to-column defaults:
--   id                UUID     PRIMARY KEY
--   listener_id       VARCHAR(255)  NOT NULL
--   event_type        VARCHAR(255)  NOT NULL
--   serialized_event  VARCHAR(4000) NOT NULL
--   publication_date  TIMESTAMPTZ   NOT NULL
--   completion_date   TIMESTAMPTZ   NULL
--
-- Lives in the `shared` schema alongside `flyway_schema_history` because
-- `application.yml` now pins Hibernate's default schema to `shared`.
-- ============================================================================

CREATE TABLE IF NOT EXISTS shared.event_publication (
    id                UUID PRIMARY KEY,
    listener_id       VARCHAR(255) NOT NULL,
    event_type        VARCHAR(255) NOT NULL,
    serialized_event  VARCHAR(4000) NOT NULL,
    publication_date  TIMESTAMPTZ NOT NULL,
    completion_date   TIMESTAMPTZ
);

-- Fast lookup of unresolved (still-uncompleted) publications during the
-- Modulith relay's replayIncompletePublications() pass.
CREATE INDEX IF NOT EXISTS ix_event_publication_completion_date
    ON shared.event_publication (completion_date);

-- Modulith looks up publications by (listener, serialized event) to detect
-- duplicates before persisting a new one.
CREATE INDEX IF NOT EXISTS ix_event_publication_listener_event
    ON shared.event_publication (listener_id, serialized_event);
