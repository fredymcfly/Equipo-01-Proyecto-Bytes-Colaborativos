-- ms-auth: user accounts.
-- Emails are stored in lowercase (the service normalizes them before saving),
-- and the CHECK constraint guarantees it at database level.
CREATE TABLE users (
    id         UUID                     PRIMARY KEY,
    username   VARCHAR(50)              NOT NULL,
    email      VARCHAR(255)             NOT NULL,
    password   VARCHAR(100)             NOT NULL,
    role       VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_email_lowercase CHECK (email = LOWER(email))
);
