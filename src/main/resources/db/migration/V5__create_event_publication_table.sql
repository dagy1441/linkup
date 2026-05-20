-- =====================================================================
-- V5 — Spring Modulith event_publication outbox table
-- =====================================================================
-- Required by spring-modulith-events-jpa: persists @ApplicationModuleListener
-- events for at-least-once delivery and recovery. Schema mirrors what Hibernate
-- auto-generates from the Modulith entity, so `ddl-auto: validate` succeeds.
--
-- Listeners marked with @ApplicationModuleListener will have their invocations
-- recorded here; failed deliveries can be republished via the Actuator endpoint
-- `/actuator/modulith/events`.

CREATE TABLE event_publication (
    id                     UUID                          NOT NULL,
    completion_attempts    INTEGER                       NOT NULL,
    completion_date        TIMESTAMP(6) WITH TIME ZONE,
    event_type             VARCHAR(255)                  NOT NULL,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    listener_id            VARCHAR(255)                  NOT NULL,
    publication_date       TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    serialized_event       VARCHAR(255)                  NOT NULL,
    status                 VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT ck_event_publication_status
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

-- Hot path: find non-completed events to republish on recovery.
CREATE INDEX ix_event_publication_completion_date ON event_publication (completion_date);
CREATE INDEX ix_event_publication_status          ON event_publication (status);
