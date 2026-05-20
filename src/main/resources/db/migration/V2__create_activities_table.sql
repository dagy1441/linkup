-- =====================================================================
-- V2 — Activities (activity bounded context)
-- =====================================================================

CREATE TABLE activities (
    id              UUID         PRIMARY KEY,
    title           VARCHAR(150) NOT NULL,
    description     VARCHAR(4000),
    city            VARCHAR(100) NOT NULL,
    address_line    VARCHAR(250),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    starts_at       TIMESTAMPTZ  NOT NULL,
    capacity        INTEGER      NOT NULL,
    booked_count    INTEGER      NOT NULL DEFAULT 0,
    organizer_id    UUID         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_activities_capacity     CHECK (capacity > 0),
    CONSTRAINT ck_activities_booked_count CHECK (booked_count >= 0 AND booked_count <= capacity),
    CONSTRAINT ck_activities_status       CHECK (status IN ('PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_activities_lat          CHECK (latitude IS NULL OR (latitude BETWEEN -90 AND 90)),
    CONSTRAINT ck_activities_lng          CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180)),
    CONSTRAINT fk_activities_organizer    FOREIGN KEY (organizer_id) REFERENCES users(id)
);

CREATE INDEX ix_activities_status_starts_at ON activities (status, starts_at);
CREATE INDEX ix_activities_organizer_id     ON activities (organizer_id);
CREATE INDEX ix_activities_city             ON activities (LOWER(city));
