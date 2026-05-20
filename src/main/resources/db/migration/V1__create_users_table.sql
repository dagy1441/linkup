-- =====================================================================
-- V1 — Local mirror of Keycloak identities (auth bounded context)
-- =====================================================================

CREATE TABLE users (
    id              UUID         PRIMARY KEY,
    keycloak_id     VARCHAR(100) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(150),
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT ck_users_status      CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX ix_users_email ON users (email);

CREATE TABLE user_roles (
    user_id UUID         NOT NULL,
    role    VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_user_roles      PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX ix_user_roles_user_id ON user_roles (user_id);
