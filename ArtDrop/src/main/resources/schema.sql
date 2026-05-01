CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    bio VARCHAR(1000),
    avatar_url VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS authority (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_authority (
    user_id BIGINT NOT NULL,
    authority_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, authority_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (authority_id) REFERENCES authority(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS artwork (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT,
    title VARCHAR(255) NOT NULL,
    medium VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    width_value DECIMAL(10, 2),
    height_value DECIMAL(10, 2),
    depth_value DECIMAL(10, 2),
    dimension_unit VARCHAR(8),
    price DECIMAL(12, 2),
    progress_status VARCHAR(20),
    sale_status VARCHAR(20),
    published_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES app_user(id)
);

ALTER TABLE artwork ADD COLUMN IF NOT EXISTS width_value DECIMAL(10, 2);
ALTER TABLE artwork ADD COLUMN IF NOT EXISTS height_value DECIMAL(10, 2);
ALTER TABLE artwork ADD COLUMN IF NOT EXISTS depth_value DECIMAL(10, 2);
ALTER TABLE artwork ADD COLUMN IF NOT EXISTS dimension_unit VARCHAR(8);
ALTER TABLE artwork DROP COLUMN IF EXISTS image_url;

CREATE TABLE IF NOT EXISTS artwork_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artwork_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (artwork_id, user_id),
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_artwork_like_artwork ON artwork_like(artwork_id);
CREATE INDEX IF NOT EXISTS idx_artwork_like_user ON artwork_like(user_id);

CREATE TABLE IF NOT EXISTS artwork_tags (
    artwork_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    UNIQUE (artwork_id, tag),
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS artwork_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artwork_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    sort_order INT DEFAULT 0,
    is_cover BOOLEAN DEFAULT FALSE,
    caption VARCHAR(255),
    created_at TIMESTAMP,
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artwork_id BIGINT NOT NULL,
    author_id BIGINT,
    text VARCHAR(2000) NOT NULL,
    parent_comment_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comment(id)
);

CREATE TABLE IF NOT EXISTS challenge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_by BIGINT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    quote VARCHAR(1000),
    kind VARCHAR(30),
    status VARCHAR(20),
    theme VARCHAR(255),
    cover_image_url VARCHAR(1000),
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_public BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS collection_artwork (
    collection_id BIGINT NOT NULL,
    artwork_id BIGINT NOT NULL,
    UNIQUE (collection_id, artwork_id),
    FOREIGN KEY (collection_id) REFERENCES collection(id) ON DELETE CASCADE,
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (follower_id, followee_id),
    FOREIGN KEY (follower_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES app_user(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_user_follow_follower ON user_follow(follower_id);
CREATE INDEX IF NOT EXISTS idx_user_follow_followee ON user_follow(followee_id);

CREATE TABLE IF NOT EXISTS challenge_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    artwork_id BIGINT NOT NULL,
    submitted_by BIGINT,
    submitted_at TIMESTAMP,
    UNIQUE (challenge_id, artwork_id),
    FOREIGN KEY (challenge_id) REFERENCES challenge(id) ON DELETE CASCADE,
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seller_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    decided_by_user_id BIGINT,
    decision_reason VARCHAR(500),
    revoked_at TIMESTAMP,
    revoked_by_user_id BIGINT,
    revoke_reason VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (decided_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    FOREIGN KEY (revoked_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_seller_application_user ON seller_application(user_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_seller_application_status ON seller_application(status, submitted_at);
