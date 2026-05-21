CREATE TABLE IF NOT EXISTS follows
(
    id           UUID                        NOT NULL,
    follower_id  UUID                        NOT NULL,
    following_id UUID                        NOT NULL,
    followed_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_follows PRIMARY KEY (id)
);

DO $$
BEGIN
    ALTER TABLE follows
    ADD CONSTRAINT uc_follows_follower_id_following_id UNIQUE (follower_id, following_id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_follows_follower_id_following_id constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_follows_follower_id_followed_at ON follows (follower_id, followed_at DESC);

CREATE INDEX IF NOT EXISTS idx_follows_following_id_followed_at ON follows (following_id, followed_at DESC);

DO $$
BEGIN
    ALTER TABLE follows
    ADD CONSTRAINT fk_follows_on_follower FOREIGN KEY (follower_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_follows_on_follower constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE follows
    ADD CONSTRAINT fk_follows_on_following FOREIGN KEY (following_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_follows_on_following constraint already exists. Ignoring...';
END $$;
