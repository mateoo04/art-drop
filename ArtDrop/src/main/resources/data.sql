INSERT INTO app_user (id, display_name, slug, bio, avatar_url, created_at, updated_at) VALUES
(1, 'Elena Rostova', 'elena-rostova', 'Abstract painter exploring light and texture.', 'https://i.pravatar.cc/160?img=47', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 'Julian Vane', 'julian-vane', 'Sculptor working in matte clay and bronze.', 'https://i.pravatar.cc/160?img=12', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 'Sarah Millay', 'sarah-millay', 'Mixed media artist based in Lisbon.', 'https://i.pravatar.cc/160?img=32', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(4, 'Marc Zheng', 'marc-zheng', '3D artist rendering tactile virtual surfaces.', 'https://i.pravatar.cc/160?img=15', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(5, 'Claire Durand', 'claire-durand', 'Heavy body acrylic pours and color theory studies.', 'https://i.pravatar.cc/160?img=49', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(6, 'Thomas Reade', 'thomas-reade', 'Architectural photographer, silver gelatin prints.', 'https://i.pravatar.cc/160?img=8', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO artwork (id, author_id, title, medium, description, image_url, price, progress_status, sale_status, published_at, like_count, created_at, updated_at) VALUES
(1, 1, 'Neon Etherealism', 'Digital', 'This piece explores the intersection of digital light and physical texture, inspired by Tokyo nights.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 1200.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(), 842, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 2, 'Quiet Form No. 4', 'Sculpture', 'Currently refining the surface tension of the curves. Aiming for a bone-like finish.', 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg', 3450.00, 'WIP', 'ORIGINAL', CURRENT_TIMESTAMP(), 1247, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 3, 'Floral Deconstruction', 'Mixed Media', 'A series exploring the fragility of botanical structures through destructive layering.', 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg', 450.00, 'FINISHED', 'EDITION', CURRENT_TIMESTAMP(), 640, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(4, 4, 'Soft Velocity', 'Digital', 'Rendered in Octane, focusing on the tactile quality of virtual light surfaces.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', 850.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(), 2412, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(5, 5, 'Oceanic Drift', 'Acrylic', 'A study in fluid dynamics and color theory using heavy body acrylics.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s', 2100.00, 'FINISHED', 'ORIGINAL', CURRENT_TIMESTAMP(), 1108, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(6, 6, 'Shadow Geometry', 'Photography', 'Limited edition of 25. Silver gelatin prints on museum-quality paper.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 550.00, 'FINISHED', 'EDITION', CURRENT_TIMESTAMP(), 450, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(7, 1, 'Golden Hour Study', 'Oil', 'Landscape inspired by sunset colors over the Adriatic.', 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 980.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -2, CURRENT_TIMESTAMP()), 312, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(8, 4, 'Pastel Recursion', 'Digital', 'Recursive pastel geometry, a meditation on loops.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', NULL, 'WIP', 'ORIGINAL', DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 88, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO artwork_tags (artwork_id, tag) VALUES
(1, 'tokyo'),
(1, 'neon'),
(1, 'abstract'),
(2, 'sculpture'),
(2, 'minimal'),
(3, 'botanical'),
(3, 'layered'),
(4, 'render'),
(4, '3d'),
(5, 'fluid'),
(5, 'color'),
(6, 'brutalism'),
(6, 'photo'),
(7, 'landscape'),
(7, 'sunset'),
(8, 'geometry'),
(8, 'pastel');

INSERT INTO artwork_image (artwork_id, image_url, sort_order, is_cover, caption, created_at) VALUES
(1, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
(2, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg', 0, TRUE, 'Main scan', CURRENT_TIMESTAMP()),
(3, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg', 0, TRUE, 'Reference shot', CURRENT_TIMESTAMP()),
(4, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
(5, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP()),
(6, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE, 'Cover image', CURRENT_TIMESTAMP());

INSERT INTO comment (artwork_id, author_id, text, parent_comment_id, created_at, updated_at, is_deleted) VALUES
(1, 2, 'Prekrasan rad!', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(1, 3, 'Nastavak serije?', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(2, 1, 'Odlican kontrast i linije.', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE);

INSERT INTO collection (id, owner_id, name, description, created_at, updated_at, is_public) VALUES
(1, 1, 'Urban Scenes', 'Street-focused sketches and photo references.', DATEADD('DAY', -95, CURRENT_TIMESTAMP()), DATEADD('DAY', -95, CURRENT_TIMESTAMP()), TRUE),
(2, 1, 'Sunset Studies', 'Warm color palette experiments for landscape compositions.', DATEADD('DAY', -77, CURRENT_TIMESTAMP()), DATEADD('DAY', -77, CURRENT_TIMESTAMP()), FALSE),
(3, 2, 'Print Drafts', 'Mixed-media drafts prepared for print review.', DATEADD('DAY', -40, CURRENT_TIMESTAMP()), DATEADD('DAY', -40, CURRENT_TIMESTAMP()), TRUE);

INSERT INTO collection_artwork (collection_id, artwork_id) VALUES
(1, 2),
(1, 3),
(2, 1),
(3, 3),
(3, 1);

INSERT INTO challenge (id, created_by, title, description, quote, kind, status, theme, cover_image_url, starts_at, ends_at, created_at, updated_at) VALUES
(1, 1, 'The Light of Dusk', 'Capturing the fleeting transition between day and night. We are looking for work that emphasizes long shadows, warm gradients, and the quiet melancholy of the blue hour.', 'Twilight is the crack between worlds where the light forgets its name and the shadows begin to sing.', 'FEATURED', 'ACTIVE', 'Dusk', NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP()), DATEADD('DAY', 11, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 1, 'Brutalist Forms', 'A study of raw materials and uncompromising geometry. We celebrate the beauty of concrete, steel, and the rhythm of repetitive structures in the urban landscape.', 'The architect''s task is to make life more beautiful, but also to show the strength that holds it up.', 'COMMUNITY_CHOICE', 'ACTIVE', 'Brutalism', NULL, DATEADD('DAY', -7, CURRENT_TIMESTAMP()), DATEADD('DAY', 7, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO challenge_submission (challenge_id, artwork_id, submitted_by, submitted_at) VALUES
(1, 1, 1, DATEADD('HOUR', -2, CURRENT_TIMESTAMP())),
(1, 7, 1, DATEADD('HOUR', -5, CURRENT_TIMESTAMP())),
(1, 5, 5, DATEADD('HOUR', -8, CURRENT_TIMESTAMP())),
(1, 4, 4, DATEADD('HOUR', -12, CURRENT_TIMESTAMP())),
(1, 3, 3, DATEADD('HOUR', -20, CURRENT_TIMESTAMP())),
(1, 8, 4, DATEADD('HOUR', -26, CURRENT_TIMESTAMP())),
(2, 6, 6, DATEADD('HOUR', -1, CURRENT_TIMESTAMP())),
(2, 2, 2, DATEADD('HOUR', -6, CURRENT_TIMESTAMP())),
(2, 4, 4, DATEADD('HOUR', -14, CURRENT_TIMESTAMP())),
(2, 1, 1, DATEADD('HOUR', -22, CURRENT_TIMESTAMP())),
(2, 5, 5, DATEADD('HOUR', -30, CURRENT_TIMESTAMP()));
