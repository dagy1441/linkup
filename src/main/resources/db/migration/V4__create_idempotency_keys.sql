-- =====================================================================
-- V4 — Idempotency keys (core)
-- =====================================================================
-- Replay cache for mutating endpoints. The composite (key, user_id, endpoint)
-- uniquely identifies a request; the same key reused by another user on a
-- different endpoint does NOT collide.

CREATE TABLE idempotency_keys (
    id                UUID         PRIMARY KEY,
    idem_key          VARCHAR(128) NOT NULL,
    user_id           UUID         NOT NULL,
    endpoint          VARCHAR(255) NOT NULL,
    request_hash      VARCHAR(64)  NOT NULL,
    response_status   SMALLINT,
    response_body     TEXT,
    response_type     VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL,
    expires_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_idempotency_keys_key_user_endpoint UNIQUE (idem_key, user_id, endpoint)
);

CREATE INDEX ix_idempotency_keys_expires_at ON idempotency_keys (expires_at);
