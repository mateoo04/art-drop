-- V7: Index for per-user active-reservation lookup (one-reservation-per-user enforcement).
CREATE INDEX idx_artwork_reserved_by_user_id
    ON artwork(reserved_by_user_id)
    WHERE reserved_by_user_id IS NOT NULL;
