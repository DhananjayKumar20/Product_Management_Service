-- Ikaansh Edutech Private Limited - Task Management Service
-- V1: users table (authentication + RBAC: USER / ADMIN)
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NULL,
    user_type  VARCHAR(50)  NULL,
    password   VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    -- unique index doubles as the lookup index for login / JWT filter (findByEmail)
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
