-- =====================================================================
-- V3 — Bookings (booking bounded context)
-- =====================================================================
-- No FK to users(id) or activities(id): cross-module FKs would tie module
-- lifecycles together and block future microservice extraction. Referential
-- integrity is enforced at the application layer via ActivitySeatService and
-- CurrentUserAccessor.

CREATE TABLE bookings (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL,
    activity_id     UUID         NOT NULL,
    seats           INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_bookings_seats  CHECK (seats > 0),
    CONSTRAINT ck_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

-- Partial unique index: at most one CONFIRMED booking per (user, activity).
-- A user can re-book the same activity only after cancelling the previous one.
CREATE UNIQUE INDEX ux_bookings_user_activity_confirmed
    ON bookings (user_id, activity_id) WHERE status = 'CONFIRMED';

CREATE INDEX ix_bookings_user_id     ON bookings (user_id);
CREATE INDEX ix_bookings_activity_id ON bookings (activity_id);
CREATE INDEX ix_bookings_status      ON bookings (status);
