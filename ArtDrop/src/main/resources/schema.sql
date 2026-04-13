CREATE TABLE IF NOT EXISTS artwork (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT,
    title VARCHAR(255) NOT NULL,
    medium VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    image_url VARCHAR(1000) NOT NULL,
    published_at TIMESTAMP,
    like_count INT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS artwork_tags (
    artwork_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
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
    artwork_id BIGINT NOT NULL,
    created_by BIGINT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    theme VARCHAR(255),
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    status VARCHAR(30),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (artwork_id) REFERENCES artwork(id) ON DELETE CASCADE
);
