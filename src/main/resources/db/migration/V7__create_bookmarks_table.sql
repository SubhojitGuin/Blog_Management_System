CREATE TABLE IF NOT EXISTS bookmarks
(
    id            UUID                        NOT NULL,
    user_id       UUID                        NOT NULL,
    post_id       UUID                        NOT NULL,
    bookmarked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_bookmarks PRIMARY KEY (id)
);

DO $$
BEGIN
    ALTER TABLE bookmarks
    ADD CONSTRAINT uc_bookmarks_user_id_post_id UNIQUE (user_id, post_id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'uc_bookmarks_user_id_post_id constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE bookmarks
    ADD CONSTRAINT fk_bookmarks_on_post FOREIGN KEY (post_id) REFERENCES posts (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_bookmarks_on_post constraint already exists. Ignoring...';
END $$;

DO $$
BEGIN
    ALTER TABLE bookmarks
    ADD CONSTRAINT fk_bookmarks_on_user FOREIGN KEY (user_id) REFERENCES users (id);
EXCEPTION
    WHEN duplicate_object THEN
        RAISE NOTICE 'fk_bookmarks_on_user constraint already exists. Ignoring...';
END $$;

CREATE INDEX IF NOT EXISTS idx_bookmarks_user_id ON bookmarks (user_id);