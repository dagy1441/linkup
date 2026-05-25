-- =====================================================================
-- V7 — Profiles (profile bounded context)
-- =====================================================================
-- One row per user. NOT linked by FK to auth.users: the link is the
-- bare user_id UUID so the two schemas can later live in different DBs
-- (CLAUDE.md §3.4, §9). UNIQUE on user_id enforces the 1:1.
--
-- DELETION_PENDING is the soft-delete state; deletion_scheduled_at is the
-- timestamp after which the daily scheduler hard-purges the row (Sprint S1
-- PR #8). The partial index keeps that scheduler cheap even when the
-- table grows.

CREATE TABLE profiles (
    id                      UUID         PRIMARY KEY,
    user_id                 UUID         NOT NULL UNIQUE,
    bio                     VARCHAR(150),
    city                    VARCHAR(100),
    date_of_birth           DATE,
    gender                  VARCHAR(20),
    photo_key               VARCHAR(255),
    status                  VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    deletion_scheduled_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_profiles_status CHECK (status IN ('ACTIVE','DELETION_PENDING')),
    CONSTRAINT ck_profiles_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','OTHER','UNDISCLOSED')),
    CONSTRAINT ck_profiles_bio_len  CHECK (bio IS NULL OR char_length(bio) <= 150),
    CONSTRAINT ck_profiles_city_len CHECK (city IS NULL OR char_length(city) <= 100)
);

-- Cheap scan path for ProfileDeletionScheduler (PR #8): only DELETION_PENDING rows.
CREATE INDEX ix_profiles_status_scheduled
    ON profiles (status, deletion_scheduled_at)
    WHERE status = 'DELETION_PENDING';
