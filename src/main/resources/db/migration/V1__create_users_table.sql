CREATE TABLE IF NOT EXISTS users
(
    id               UUID                        NOT NULL,
    name             VARCHAR(255),
    username         VARCHAR(30),
    password         VARCHAR(255),
    email            VARCHAR(320),
    bio              VARCHAR(255),
    gender           VARCHAR(20),
    date_of_birth    date,
    no_of_followers  INTEGER                     NOT NULL,
    no_of_followings INTEGER                     NOT NULL,
    no_of_posts      INTEGER                     NOT NULL,
    active           BOOLEAN                     NOT NULL,
    is_deleted       BOOLEAN                     NOT NULL,
    token_version    INTEGER                     NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_entity_roles
(
    user_entity_id UUID NOT NULL,
    roles          VARCHAR(255)
);

DO $$
BEGIN
  ALTER TABLE user_entity_roles
    ADD CONSTRAINT uc_user_entity_roles UNIQUE (user_entity_id, roles);
EXCEPTION
  WHEN duplicate_object THEN
    RAISE NOTICE 'uc_user_entity_roles constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE user_entity_roles
        ADD CONSTRAINT fk_user_entity_roles_on_user_entity FOREIGN KEY (user_entity_id) REFERENCES users (id)
        ON DELETE CASCADE;
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_user_entity_roles_on_user_entity constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE users
        ADD CONSTRAINT uc_users_email UNIQUE (email);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_users_email constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE users
      ADD CONSTRAINT uc_users_username UNIQUE (username);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_users_username constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_users_deleted_active ON USERS (IS_DELETED, ACTIVE);
