INSERT INTO artwork (id, author_id, title, medium, description, image_url, published_at, like_count, created_at, updated_at) VALUES
(1, 1, 'Golden Hour', 'Digital Painting', 'Landscape inspired by sunset colors.', 'https://example.com/images/golden-hour.jpg', CURRENT_TIMESTAMP(), 125, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 2, 'Urban Sketch', 'Traditional Ink', 'Quick ink sketch of a city street.', 'https://example.com/images/urban-sketch.jpg', CURRENT_TIMESTAMP(), 87, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 2, 'Rural Sketch', 'Digital Photo', 'Reference composition study.', 'https://example.com/images/rural-sketch.jpg', CURRENT_TIMESTAMP(), 42, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO artwork_tags (artwork_id, tag) VALUES
(1, 'sunset'),
(1, 'landscape'),
(2, 'city'),
(2, 'ink'),
(3, 'photo');

INSERT INTO artwork_image (artwork_id, image_url, sort_order, is_cover, caption, created_at) VALUES
(1, 'https://example.com/images/golden-hour.jpg', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
(1, 'https://example.com/images/golden-hour-closeup.jpg', 1, FALSE, 'Brush texture close-up', CURRENT_TIMESTAMP()),
(2, 'https://example.com/images/urban-sketch.jpg', 0, TRUE, 'Main scan', CURRENT_TIMESTAMP()),
(3, 'https://example.com/images/rural-sketch.jpg', 0, TRUE, 'Reference shot', CURRENT_TIMESTAMP());

INSERT INTO comment (artwork_id, author_id, text, parent_comment_id, created_at, updated_at, is_deleted) VALUES
(1, 2, 'Prekrasan rad!', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(1, 3, 'Nastavak serije?', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(2, 1, 'Odlican kontrast i linije.', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE);

INSERT INTO challenge (artwork_id, created_by, title, description, theme, starts_at, ends_at, status, created_at, updated_at) VALUES
(1, 1, 'Weekly Prompt #1', 'Create a warm color composition.', 'Warm Palette', CURRENT_TIMESTAMP(), DATEADD('DAY', 7, CURRENT_TIMESTAMP()), 'ACTIVE', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 1, 'Urban Motion', 'Capture movement in urban scenes.', 'City Dynamics', CURRENT_TIMESTAMP(), DATEADD('DAY', 10, CURRENT_TIMESTAMP()), 'ACTIVE', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(1, 2, 'Sunset Variations', 'Try three different sunset moods.', 'Sunset Study', DATEADD('DAY', -14, CURRENT_TIMESTAMP()), DATEADD('DAY', -7, CURRENT_TIMESTAMP()), 'ENDED', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
