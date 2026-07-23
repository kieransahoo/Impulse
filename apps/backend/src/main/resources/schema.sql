CREATE EXTENSION IF NOT EXISTS vector;

-- Hibernate ddl-auto can create enum checks but does not widen an existing
-- constraint when a Kotlin enum gains a value. Keep this idempotent for the
-- migration-free local MVP so existing databases accept generic web memories.
ALTER TABLE IF EXISTS memories
    DROP CONSTRAINT IF EXISTS memories_platform_check;

ALTER TABLE IF EXISTS memories
    ADD CONSTRAINT memories_platform_check
    CHECK (platform IN ('YOUTUBE_PLAYLIST', 'YOUTUBE_VIDEO', 'INSTAGRAM', 'WEB'));
