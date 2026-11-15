-- Additive migration: apply BEFORE deploying the API with thumbnailCode.
-- Run psql with ON_ERROR_STOP=1 after a verified pg_dump.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE photo ADD COLUMN IF NOT EXISTS thumbnail_code VARCHAR(1024);
ALTER TABLE photo ADD COLUMN IF NOT EXISTS display_code VARCHAR(1024);
CREATE TABLE IF NOT EXISTS thumbnail_migration_item (
    run_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    photo_id BIGINT NOT NULL,
    source_key TEXT NOT NULL,
    source_etag TEXT NOT NULL,
    source_size BIGINT NOT NULL,
    backup_key TEXT NOT NULL,
    thumbnail_key TEXT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (run_id, photo_id)
);
ALTER TABLE thumbnail_migration_item ADD COLUMN IF NOT EXISTS asset_kind VARCHAR(20) NOT NULL DEFAULT 'thumbnail';
-- No FK: rollback evidence survives a user's deletion of a photo.
COMMIT;
