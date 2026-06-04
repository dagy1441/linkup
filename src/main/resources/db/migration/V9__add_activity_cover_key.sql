-- =====================================================================
-- V9 — Activity cover image
-- =====================================================================
-- Organizers can attach a cover photo to their activity (US-021 / Phase F4
-- frontend). The image lives in MinIO / S3 under
--   linkup-activity-covers/activities/<activityId>/cover.<ext>
-- and we persist only the storage key here; the presigned URL is resolved
-- by the controller on each read.

ALTER TABLE activities ADD COLUMN cover_key VARCHAR(255);
