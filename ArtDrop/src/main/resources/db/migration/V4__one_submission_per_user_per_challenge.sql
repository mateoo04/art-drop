-- Enforces "one entry per user per challenge" at the DB level.
-- Cleans up any pre-existing duplicates first by keeping the earliest
-- submission (lowest id) per (challenge_id, submitted_by) pair.

DELETE FROM challenge_submission s
WHERE EXISTS (
    SELECT 1 FROM challenge_submission older
    WHERE older.challenge_id = s.challenge_id
      AND older.submitted_by = s.submitted_by
      AND older.id < s.id
);

ALTER TABLE challenge_submission
    ADD CONSTRAINT uq_challenge_submission_user_per_challenge
    UNIQUE (challenge_id, submitted_by);
