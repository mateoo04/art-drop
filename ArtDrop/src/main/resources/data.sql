INSERT INTO app_user (username, email, password_hash, display_name, slug, bio, avatar_url, created_at, updated_at) VALUES
('admin', 'admin@artdrop.local', '$2y$12$SuJuEno6LV0dzleXdaMelOWXcvOk4BgNpoEVYspTjfplrl34/jrA2', 'Elena Rostova', 'elena-rostova', 'Abstract painter exploring light and texture.', 'https://i.pravatar.cc/160?img=47', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('user', 'user@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Julian Vane', 'julian-vane', 'Sculptor working in matte clay and bronze.', 'https://i.pravatar.cc/160?img=12', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('sarah', 'sarah@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Sarah Millay', 'sarah-millay', 'Mixed media artist based in Lisbon.', 'https://i.pravatar.cc/160?img=32', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('marc', 'marc@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Marc Zheng', 'marc-zheng', '3D artist rendering tactile virtual surfaces.', 'https://i.pravatar.cc/160?img=15', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('claire', 'claire@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Claire Durand', 'claire-durand', 'Heavy body acrylic pours and color theory studies.', 'https://i.pravatar.cc/160?img=49', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
('thomas', 'thomas@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Thomas Reade', 'thomas-reade', 'Architectural photographer, silver gelatin prints.', 'https://i.pravatar.cc/160?img=8', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO authority (name) VALUES
('ROLE_ADMIN'),
('ROLE_USER');

INSERT INTO user_authority (user_id, authority_id) VALUES
((SELECT id FROM app_user WHERE username = 'admin'), (SELECT id FROM authority WHERE name = 'ROLE_ADMIN')),
((SELECT id FROM app_user WHERE username = 'admin'), (SELECT id FROM authority WHERE name = 'ROLE_USER')),
((SELECT id FROM app_user WHERE username = 'user'), (SELECT id FROM authority WHERE name = 'ROLE_USER')),
((SELECT id FROM app_user WHERE username = 'sarah'), (SELECT id FROM authority WHERE name = 'ROLE_USER')),
((SELECT id FROM app_user WHERE username = 'marc'), (SELECT id FROM authority WHERE name = 'ROLE_USER')),
((SELECT id FROM app_user WHERE username = 'claire'), (SELECT id FROM authority WHERE name = 'ROLE_USER')),
((SELECT id FROM app_user WHERE username = 'thomas'), (SELECT id FROM authority WHERE name = 'ROLE_USER'));

INSERT INTO artwork (author_id, title, medium, description, image_url, price, progress_status, sale_status, published_at, like_count, created_at, updated_at) VALUES
((SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Neon Etherealism', 'Digital', 'This piece explores the intersection of digital light and physical texture, inspired by Tokyo nights.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 1200.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(), 842, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'julian-vane'), 'Quiet Form No. 4', 'Sculpture', 'Currently refining the surface tension of the curves. Aiming for a bone-like finish.', 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg', 3450.00, 'WIP', 'ORIGINAL', CURRENT_TIMESTAMP(), 1247, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'sarah-millay'), 'Floral Deconstruction', 'Mixed Media', 'A series exploring the fragility of botanical structures through destructive layering.', 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg', 450.00, 'FINISHED', 'EDITION', CURRENT_TIMESTAMP(), 640, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'marc-zheng'), 'Soft Velocity', 'Digital', 'Rendered in Octane, focusing on the tactile quality of virtual light surfaces.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', 850.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(), 2412, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'claire-durand'), 'Oceanic Drift', 'Acrylic', 'A study in fluid dynamics and color theory using heavy body acrylics.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s', 2100.00, 'FINISHED', 'ORIGINAL', CURRENT_TIMESTAMP(), 1108, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'thomas-reade'), 'Shadow Geometry', 'Photography', 'Limited edition of 25. Silver gelatin prints on museum-quality paper.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 550.00, 'FINISHED', 'EDITION', CURRENT_TIMESTAMP(), 450, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Golden Hour Study', 'Oil', 'Landscape inspired by sunset colors over the Adriatic.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 980.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 312, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
((SELECT id FROM app_user WHERE slug = 'marc-zheng'), 'Pastel Recursion', 'Digital', 'Recursive pastel geometry, a meditation on loops.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', NULL, 'WIP', 'ORIGINAL', DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 88, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO artwork_tags (artwork_id, tag) VALUES
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), 'tokyo'),
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), 'neon'),
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), 'abstract'),
((SELECT id FROM artwork WHERE title = 'Quiet Form No. 4'), 'sculpture'),
((SELECT id FROM artwork WHERE title = 'Quiet Form No. 4'), 'minimal'),
((SELECT id FROM artwork WHERE title = 'Floral Deconstruction'), 'botanical'),
((SELECT id FROM artwork WHERE title = 'Floral Deconstruction'), 'layered'),
((SELECT id FROM artwork WHERE title = 'Soft Velocity'), 'render'),
((SELECT id FROM artwork WHERE title = 'Soft Velocity'), '3d'),
((SELECT id FROM artwork WHERE title = 'Oceanic Drift'), 'fluid'),
((SELECT id FROM artwork WHERE title = 'Oceanic Drift'), 'color'),
((SELECT id FROM artwork WHERE title = 'Shadow Geometry'), 'brutalism'),
((SELECT id FROM artwork WHERE title = 'Shadow Geometry'), 'photo'),
((SELECT id FROM artwork WHERE title = 'Golden Hour Study'), 'landscape'),
((SELECT id FROM artwork WHERE title = 'Golden Hour Study'), 'sunset'),
((SELECT id FROM artwork WHERE title = 'Pastel Recursion'), 'geometry'),
((SELECT id FROM artwork WHERE title = 'Pastel Recursion'), 'pastel');

INSERT INTO artwork_image (artwork_id, image_url, sort_order, is_cover, caption, created_at) VALUES
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
((SELECT id FROM artwork WHERE title = 'Quiet Form No. 4'), 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg', 0, TRUE, 'Main scan', CURRENT_TIMESTAMP()),
((SELECT id FROM artwork WHERE title = 'Floral Deconstruction'), 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg', 0, TRUE, 'Reference shot', CURRENT_TIMESTAMP()),
((SELECT id FROM artwork WHERE title = 'Soft Velocity'), 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
((SELECT id FROM artwork WHERE title = 'Oceanic Drift'), 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
((SELECT id FROM artwork WHERE title = 'Shadow Geometry'), 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP());

INSERT INTO comment (artwork_id, author_id, text, parent_comment_id, created_at, updated_at, is_deleted) VALUES
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), (SELECT id FROM app_user WHERE slug = 'julian-vane'), 'Prekrasan rad!', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
((SELECT id FROM artwork WHERE title = 'Neon Etherealism'), (SELECT id FROM app_user WHERE slug = 'sarah-millay'), 'Nastavak serije?', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
((SELECT id FROM artwork WHERE title = 'Quiet Form No. 4'), (SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Odlican kontrast i linije.', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE);

INSERT INTO collection (id, owner_id, name, description, created_at, updated_at, is_public) VALUES
(1, (SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Urban Scenes', 'Street-focused sketches and photo references.', DATEADD('DAY', -95, CURRENT_TIMESTAMP()), DATEADD('DAY', -95, CURRENT_TIMESTAMP()), TRUE),
(2, (SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Sunset Studies', 'Warm color palette experiments for landscape compositions.', DATEADD('DAY', -77, CURRENT_TIMESTAMP()), DATEADD('DAY', -77, CURRENT_TIMESTAMP()), FALSE),
(3, (SELECT id FROM app_user WHERE slug = 'julian-vane'), 'Print Drafts', 'Mixed-media drafts prepared for print review.', DATEADD('DAY', -40, CURRENT_TIMESTAMP()), DATEADD('DAY', -40, CURRENT_TIMESTAMP()), TRUE);

INSERT INTO collection_artwork (collection_id, artwork_id) VALUES
(1, (SELECT id FROM artwork WHERE title = 'Quiet Form No. 4')),
(1, (SELECT id FROM artwork WHERE title = 'Floral Deconstruction')),
(2, (SELECT id FROM artwork WHERE title = 'Neon Etherealism')),
(3, (SELECT id FROM artwork WHERE title = 'Floral Deconstruction')),
(3, (SELECT id FROM artwork WHERE title = 'Neon Etherealism'));

INSERT INTO challenge (id, created_by, title, description, quote, kind, status, theme, cover_image_url, starts_at, ends_at, created_at, updated_at) VALUES
(1, (SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'The Light of Dusk', 'Capturing the fleeting transition between day and night. We are looking for work that emphasizes long shadows, warm gradients, and the quiet melancholy of the blue hour.', 'Twilight is the crack between worlds where the light forgets its name and the shadows begin to sing.', 'FEATURED', 'ACTIVE', 'Dusk', NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP()), DATEADD('DAY', 11, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, (SELECT id FROM app_user WHERE slug = 'elena-rostova'), 'Brutalist Forms', 'A study of raw materials and uncompromising geometry. We celebrate the beauty of concrete, steel, and the rhythm of repetitive structures in the urban landscape.', 'The architect''s task is to make life more beautiful, but also to show the strength that holds it up.', 'COMMUNITY_CHOICE', 'ACTIVE', 'Brutalism', NULL, DATEADD('DAY', -7, CURRENT_TIMESTAMP()), DATEADD('DAY', 7, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO challenge_submission (challenge_id, artwork_id, submitted_by, submitted_at) VALUES
(1, (SELECT id FROM artwork WHERE title = 'Neon Etherealism'), (SELECT id FROM app_user WHERE slug = 'elena-rostova'), DATEADD('HOUR', -2, CURRENT_TIMESTAMP())),
(1, (SELECT id FROM artwork WHERE title = 'Golden Hour Study'), (SELECT id FROM app_user WHERE slug = 'elena-rostova'), DATEADD('HOUR', -5, CURRENT_TIMESTAMP())),
(1, (SELECT id FROM artwork WHERE title = 'Oceanic Drift'), (SELECT id FROM app_user WHERE slug = 'claire-durand'), DATEADD('HOUR', -8, CURRENT_TIMESTAMP())),
(1, (SELECT id FROM artwork WHERE title = 'Soft Velocity'), (SELECT id FROM app_user WHERE slug = 'marc-zheng'), DATEADD('HOUR', -12, CURRENT_TIMESTAMP())),
(1, (SELECT id FROM artwork WHERE title = 'Floral Deconstruction'), (SELECT id FROM app_user WHERE slug = 'sarah-millay'), DATEADD('HOUR', -20, CURRENT_TIMESTAMP())),
(1, (SELECT id FROM artwork WHERE title = 'Pastel Recursion'), (SELECT id FROM app_user WHERE slug = 'marc-zheng'), DATEADD('HOUR', -26, CURRENT_TIMESTAMP())),
(2, (SELECT id FROM artwork WHERE title = 'Shadow Geometry'), (SELECT id FROM app_user WHERE slug = 'thomas-reade'), DATEADD('HOUR', -1, CURRENT_TIMESTAMP())),
(2, (SELECT id FROM artwork WHERE title = 'Quiet Form No. 4'), (SELECT id FROM app_user WHERE slug = 'julian-vane'), DATEADD('HOUR', -6, CURRENT_TIMESTAMP())),
(2, (SELECT id FROM artwork WHERE title = 'Soft Velocity'), (SELECT id FROM app_user WHERE slug = 'marc-zheng'), DATEADD('HOUR', -14, CURRENT_TIMESTAMP())),
(2, (SELECT id FROM artwork WHERE title = 'Neon Etherealism'), (SELECT id FROM app_user WHERE slug = 'elena-rostova'), DATEADD('HOUR', -22, CURRENT_TIMESTAMP())),
(2, (SELECT id FROM artwork WHERE title = 'Oceanic Drift'), (SELECT id FROM app_user WHERE slug = 'claire-durand'), DATEADD('HOUR', -30, CURRENT_TIMESTAMP()));

ALTER TABLE collection ALTER COLUMN id RESTART WITH 100;
ALTER TABLE challenge ALTER COLUMN id RESTART WITH 100;
