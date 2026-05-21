CREATE TABLE IF NOT EXISTS posts
(
    id                   UUID                        NOT NULL,
    title                VARCHAR(100)                NOT NULL,
    slug                 VARCHAR(100)                NOT NULL,
    description          VARCHAR(2000)               NOT NULL,
    content              TEXT                        NOT NULL,
    reading_time_minutes INTEGER                     NOT NULL,
    user_id              UUID                        NOT NULL,
    category_id          UUID                        NOT NULL,
    like_count           INTEGER                     NOT NULL,
    comment_count        INTEGER                     NOT NULL,
    view_count           BIGINT                      NOT NULL,
    status               VARCHAR(20)                 NOT NULL,
    publish_at           TIMESTAMP WITHOUT TIME ZONE,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_posts PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_posts_category_created_at ON posts (category_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_posts_status_publish_at ON posts (status, publish_at);

CREATE INDEX IF NOT EXISTS idx_posts_user_created_at ON posts (user_id, created_at DESC);

DO $$
BEGIN
    ALTER TABLE posts
    ADD CONSTRAINT fk_posts_on_category FOREIGN KEY (category_id) REFERENCES categories (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_posts_on_category constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE posts
    ADD CONSTRAINT fk_posts_on_user FOREIGN KEY (user_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_posts_on_user constraint already exists. Ignoring...';
END $$;
