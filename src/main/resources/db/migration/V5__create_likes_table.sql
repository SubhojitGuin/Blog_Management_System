CREATE TABLE IF NOT EXISTS likes
(
    id       UUID                        NOT NULL,
    user_id  UUID                        NOT NULL,
    post_id  UUID                        NOT NULL,
    liked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_likes PRIMARY KEY (id)
);

DO $$
BEGIN
    ALTER TABLE likes
    ADD CONSTRAINT uc_likes_user_id_post_id UNIQUE (user_id, post_id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_likes_user_id_post_id constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE likes
    ADD CONSTRAINT fk_likes_on_post FOREIGN KEY (post_id) REFERENCES posts (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_likes_on_post constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_likes_post_id ON likes (post_id);

DO $$
BEGIN
    ALTER TABLE likes
    ADD CONSTRAINT fk_likes_on_user FOREIGN KEY (user_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_likes_on_user constraint already exists. Ignoring...';
END $$;

