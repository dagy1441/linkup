-- =====================================================================
-- V6 — Index optimization (post-S0 audit)
-- =====================================================================
-- Two findings from docs/code-review-S0.md:
--
-- M-2: BookingQueryService.listMine runs
--          WHERE user_id = ? AND status = ? ORDER BY created_at DESC
--      The previous V3 had 3 single-column indexes, forcing Postgres to
--      pick one and filter/sort in memory — seq-ish on power-users.
--      A composite (user_id, status, created_at DESC) lets the planner
--      stream rows already filtered + ordered.
--
-- M-3: ActivityQueryService.findPublishedUpcomingByCity runs
--          WHERE LOWER(city) = :city
--      The previous V2 index on `city` (plain column) doesn't help — the
--      LOWER() wrapper makes it an expression mismatch. A functional
--      index on LOWER(city) is what's actually queried.
--
-- Both replacements are safe: the new indexes cover all queries that the
-- old ones served, so the DROPs do not regress any read path.
-- =====================================================================

-- M-2: composite for "my bookings, filtered by status, newest first"
CREATE INDEX ix_bookings_user_status_created
    ON bookings (user_id, status, created_at DESC);

-- The two single-column indexes are now redundant for the listMine path
-- and not used elsewhere. Drop to free up write overhead.
DROP INDEX IF EXISTS ix_bookings_user_id;
DROP INDEX IF EXISTS ix_bookings_status;

-- M-3: functional index matching the JPQL LOWER(city) predicate
CREATE INDEX ix_activities_city_lower ON activities (LOWER(city));

-- The previous plain-column index never matched the query — safe to drop.
DROP INDEX IF EXISTS ix_activities_city;
