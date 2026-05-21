CREATE TABLE IF NOT EXISTS comments
(
    id         UUID                        NOT NULL,
    body       VARCHAR(1000)               NOT NULL,
    user_id    UUID                        NOT NULL,
    post_id    UUID                        NOT NULL,
    parent_id  UUID,
    depth      INTEGER                     NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_comments_post_depth ON comments (post_id, depth);

DO $$
BEGIN
    ALTER TABLE comments
    ADD CONSTRAINT fk_comments_on_parent FOREIGN KEY (parent_id) REFERENCES comments (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_comments_on_parent constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments (parent_id);

DO $$
BEGIN
    ALTER TABLE comments
    ADD CONSTRAINT fk_comments_on_post FOREIGN KEY (post_id) REFERENCES posts (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_comments_on_post constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE comments
    ADD CONSTRAINT fk_comments_on_user FOREIGN KEY (user_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_comments_on_user constraint already exists. Ignoring...';
END $$;



