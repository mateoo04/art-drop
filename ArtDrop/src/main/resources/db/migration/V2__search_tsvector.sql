-- Full-text search via tsvector + GIN, with pg_trgm fuzzy match for short fields.
-- pg_trgm extension is created in V1.

-- ARTWORK: weighted tsvector across title (A), description (B), medium (C).
-- Tags live in artwork_tags and are searched via a separate trigram index (joined in the query).
ALTER TABLE artwork ADD COLUMN search_tsv tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title,       '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(medium,      '')), 'C')
    ) STORED;
CREATE INDEX idx_artwork_search_tsv ON artwork USING GIN (search_tsv);

-- Tag fuzzy match (substring + typo tolerance via trigram).
CREATE INDEX idx_artwork_tags_tag_trgm ON artwork_tags USING GIN (tag gin_trgm_ops);

-- USERS: short fields, FTS adds nothing useful. Trigram fuzzy match instead.
CREATE INDEX idx_app_user_display_name_trgm ON app_user USING GIN (display_name gin_trgm_ops);
CREATE INDEX idx_app_user_slug_trgm         ON app_user USING GIN (slug         gin_trgm_ops);
CREATE INDEX idx_app_user_username_trgm     ON app_user USING GIN (username     gin_trgm_ops);

-- CHALLENGE: weighted tsvector across title (A), description (B), theme (C).
ALTER TABLE challenge ADD COLUMN search_tsv tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title,       '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(theme,       '')), 'C')
    ) STORED;
CREATE INDEX idx_challenge_search_tsv ON challenge USING GIN (search_tsv);
