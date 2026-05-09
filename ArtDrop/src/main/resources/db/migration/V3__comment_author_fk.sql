-- Add foreign key from comment.author_id to app_user.id.
-- Orphans (author_id pointing at a deleted/missing user) are nulled out so the constraint can be added.
-- ON DELETE SET NULL keeps comments visible after a user is deleted.

UPDATE comment
   SET author_id = NULL
 WHERE author_id IS NOT NULL
   AND author_id NOT IN (SELECT id FROM app_user);

ALTER TABLE comment
    ADD CONSTRAINT fk_comment_author
    FOREIGN KEY (author_id) REFERENCES app_user(id) ON DELETE SET NULL;
