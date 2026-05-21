CREATE TABLE IF NOT EXISTS categories
(
    id          UUID                        NOT NULL,
    name        VARCHAR(100)                NOT NULL,
    description VARCHAR(500)                NOT NULL,
    slug        VARCHAR(100)                NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

DO $$
BEGIN
    ALTER TABLE categories
    ADD CONSTRAINT uc_categories_name UNIQUE (name);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_categories_name constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE categories
    ADD CONSTRAINT uc_categories_slug UNIQUE (slug);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_categories_slug constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories (slug);
