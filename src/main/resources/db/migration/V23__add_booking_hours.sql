-- A booking can be for part of a day. The start and end time are empty for a booking of the whole day (all earlier ones).
-- Several bookings can share a day as long as their hours do not overlap.
ALTER TABLE venue_bookings ADD COLUMN start_time TIME, ADD COLUMN end_time TIME;

-- The same member may now ask for different hours of one day, so duplicates are checked by the application.
DROP INDEX uq_venue_bookings_pending;
CREATE INDEX idx_venue_bookings_day ON venue_bookings (venue_id, day_date, status);
