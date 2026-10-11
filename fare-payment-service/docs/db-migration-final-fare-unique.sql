-- Fare & Payment Service: one FINAL fare per ride, enforced by the database.
--
-- WHO NEEDS THIS: only a database that already contains fare_records rows created BEFORE this
-- change (e.g. your local farepaymentdb). A brand-new database needs nothing - Hibernate
-- (spring.jpa.hibernate.ddl-auto=update) creates the column and constraint on first start.
-- If you have no data you care about, the simplest route is:
--     DROP DATABASE farepaymentdb;  CREATE DATABASE farepaymentdb;
--
-- HOW IT WORKS: fare_records.final_ride_id holds ride_id for FINAL rows and NULL for every other
-- row, and has a UNIQUE constraint. NULLs never collide, so many ESTIMATED rows per ride stay legal.
-- The application fills the column automatically for new rows; this script fills it for old rows.
--
-- Run these steps in order (PostgreSQL syntax).

-- STEP 1 - look for rides that already have more than one FINAL fare. Expect zero rows.
SELECT ride_id, COUNT(*) AS final_rows, ARRAY_AGG(id ORDER BY id) AS fare_ids
FROM fare_records
WHERE fare_type = 'FINAL'
GROUP BY ride_id
HAVING COUNT(*) > 1;

-- STEP 2 - if STEP 1 returned rows, resolve each ride by hand BEFORE continuing: keep the fare that
-- payment_records.fare_id points at (if any), otherwise the oldest, and delete the others, e.g.
--     DELETE FROM fare_records WHERE id = <duplicate fare id>;
-- Never delete a fare that a payment_records row references.
SELECT f.id AS fare_id, f.ride_id, p.payment_id, p.status
FROM fare_records f
LEFT JOIN payment_records p ON p.fare_id = f.id
WHERE f.fare_type = 'FINAL'
ORDER BY f.ride_id, f.id;

-- STEP 3 - backfill the marker for existing FINAL rows. If duplicates remain this fails with a
-- unique-violation error, which is the signal to go back to STEP 2.
UPDATE fare_records
SET final_ride_id = ride_id
WHERE fare_type = 'FINAL'
  AND final_ride_id IS NULL;

-- STEP 4 - confirm the constraint exists (Hibernate creates it on startup; create it here only if
-- the query returns no row).
SELECT conname FROM pg_constraint WHERE conname = 'uk_fare_records_final_ride_id';
-- ALTER TABLE fare_records
--     ADD CONSTRAINT uk_fare_records_final_ride_id UNIQUE (final_ride_id);
