CREATE TABLE featured_challenge (
    id                       SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    current_challenge_id     BIGINT REFERENCES challenge(id) ON DELETE SET NULL,
    next_challenge_id        BIGINT REFERENCES challenge(id) ON DELETE SET NULL,
    trigger_type             VARCHAR(20),
    trigger_at               TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT featured_challenge_trigger_at_required
        CHECK (trigger_type IS DISTINCT FROM 'AT_TIME' OR trigger_at IS NOT NULL),
    CONSTRAINT featured_challenge_trigger_type_valid
        CHECK (trigger_type IS NULL OR trigger_type IN ('AT_TIME', 'WHEN_CURRENT_ENDS'))
);

INSERT INTO featured_challenge (id) VALUES (1);

UPDATE featured_challenge
SET current_challenge_id = (
    SELECT id FROM challenge
    WHERE kind = 'FEATURED' AND status = 'ACTIVE'
    ORDER BY starts_at DESC NULLS LAST
    LIMIT 1
)
WHERE id = 1;

ALTER TABLE challenge DROP COLUMN kind;
