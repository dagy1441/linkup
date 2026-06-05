-- =====================================================================
-- V10 — Add category to activities (Sprint S3.5-A)
-- =====================================================================
-- A category is mandatory at the domain level, but existing rows (created
-- before this migration) must be backfilled. We default to CULTURE — the
-- most generic bucket — and organizers can re-categorize from the edit page.
--
-- Steps:
--   1. Add column NULL-able so the backfill can complete without violating NOT NULL.
--   2. Backfill all existing rows to CULTURE.
--   3. Tighten the column to NOT NULL.
--   4. Add the CHECK constraint enumerating allowed values.
--   5. Index for the filter-by-category endpoint coming in the same sprint.

ALTER TABLE activities ADD COLUMN category VARCHAR(20);

UPDATE activities SET category = 'CULTURE' WHERE category IS NULL;

ALTER TABLE activities ALTER COLUMN category SET NOT NULL;

ALTER TABLE activities ADD CONSTRAINT ck_activities_category
    CHECK (category IN (
        'CULTURE', 'FORMATION', 'SOIREE', 'TOURISME', 'SPORT',
        'FESTIVAL', 'SCIENCE', 'GASTRONOMIE', 'BUSINESS'
    ));

CREATE INDEX ix_activities_category_starts_at ON activities (category, starts_at);
