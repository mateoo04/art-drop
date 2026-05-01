INSERT INTO app_user (id, username, email, password_hash, display_name, slug, bio, avatar_url, created_at, updated_at) VALUES
(1, 'admin', 'admin@artdrop.local', '$2y$12$SuJuEno6LV0dzleXdaMelOWXcvOk4BgNpoEVYspTjfplrl34/jrA2', 'Elena Rostova', 'elena-rostova', 'Abstract painter exploring light and texture.', 'https://i.pravatar.cc/160?img=47', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 'user', 'user@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Julian Vane', 'julian-vane', 'Sculptor working in matte clay and bronze.', 'https://i.pravatar.cc/160?img=12', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3, 'sarah', 'sarah@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Sarah Millay', 'sarah-millay', 'Mixed media artist based in Lisbon.', 'https://i.pravatar.cc/160?img=32', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(4, 'marc', 'marc@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Marc Zheng', 'marc-zheng', '3D artist rendering tactile virtual surfaces.', 'https://i.pravatar.cc/160?img=15', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(5, 'claire', 'claire@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Claire Durand', 'claire-durand', 'Heavy body acrylic pours and color theory studies.', 'https://i.pravatar.cc/160?img=49', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(6, 'thomas', 'thomas@artdrop.local', '$2y$12$OYLYVeDx36mPNwI6gK55K.9Ocq/OvVhiJD47KT3dEt5RYnL9Vy4cS', 'Thomas Reade', 'thomas-reade', 'Architectural photographer, silver gelatin prints.', 'https://i.pravatar.cc/160?img=8', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO authority (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_USER');

INSERT INTO user_authority (user_id, authority_id) VALUES
(1, 1),
(1, 2),
(2, 2),
(3, 2),
(4, 2),
(5, 2),
(6, 2);

INSERT INTO artwork (id, author_id, title, medium, description, width_value, height_value, depth_value, dimension_unit, price, progress_status, sale_status, published_at, created_at, updated_at) VALUES
(1,  1, 'Neon Etherealism',     'Digital',     'This piece explores the intersection of digital light and physical texture, inspired by Tokyo nights.', NULL,   NULL,   NULL,  NULL,  1200.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2,  2, 'Quiet Form No. 4',     'Sculpture',   'Currently refining the surface tension of the curves. Aiming for a bone-like finish.',                40.00,  55.00,  30.00, 'CM',  3450.00, 'WIP',      'ORIGINAL',  CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(3,  3, 'Floral Deconstruction','Mixed Media', 'A series exploring the fragility of botanical structures through destructive layering.',             50.00,  70.00,  NULL,  'CM',   450.00, 'FINISHED', 'EDITION',   CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(4,  4, 'Soft Velocity',        'Digital',     'Rendered in Octane, focusing on the tactile quality of virtual light surfaces.',                      NULL,   NULL,   NULL,  NULL,   850.00, 'FINISHED', 'AVAILABLE', CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(5,  5, 'Oceanic Drift',        'Acrylic',     'A study in fluid dynamics and color theory using heavy body acrylics.',                                90.00, 120.00,  NULL,  'CM',  2100.00, 'FINISHED', 'ORIGINAL',  CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(6,  6, 'Shadow Geometry',      'Photography', 'Limited edition of 25. Silver gelatin prints on museum-quality paper.',                                40.00,  50.00,  NULL,  'CM',   550.00, 'FINISHED', 'EDITION',   CURRENT_TIMESTAMP(),                         CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(7,  1, 'Golden Hour Study',    'Oil',         'Landscape inspired by sunset colors over the Adriatic.',                                              60.00,  45.00,  NULL,  'CM',   980.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY',  -2, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(8,  4, 'Pastel Recursion',     'Digital',     'Recursive pastel geometry, a meditation on loops.',                                                    NULL,   NULL,   NULL,  NULL,     NULL, 'WIP',      NULL,        DATEADD('DAY',  -1, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(9,  1, 'Crimson Threshold',    'Oil',         'Heavy impasto in a single warm key, painted in three sittings.',                                       80.00, 100.00,  NULL,  'CM',   720.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY',  -4, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(10, 3, 'Static Bloom',         'Mixed Media', 'Photogravure layered with cyanotype, examining radio static as botany.',                               30.00,  40.00,  NULL,  'CM',   390.00, 'FINISHED', 'EDITION',   DATEADD('DAY',  -6, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(11, 6, 'Brutalist Echo',       'Photography', 'A study of poured-concrete stairwells in early-morning fog.',                                          50.00,  70.00,  NULL,  'CM',   620.00, 'FINISHED', 'EDITION',   DATEADD('DAY',  -7, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(12, 2, 'Iron and Cloud',       'Sculpture',   'Hot-rolled steel offcuts welded into a single suspended form.',                                        80.00, 140.00,  60.00, 'CM',  4100.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY',  -9, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(13, 4, 'Neon Threshold',       'Digital',     'Companion piece to Soft Velocity, rendered in a single overnight pass.',                               NULL,   NULL,   NULL,  NULL,   680.00, 'WIP',      'ORIGINAL',  DATEADD('DAY', -10, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(14, 5, 'Folded Light',         'Acrylic',     'Fluid pour over folded canvas, tilted twice during cure.',                                            100.00, 150.00,  NULL,  'CM',  1450.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -11, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(15, 1, 'Twilight Loop',        'Digital',     'A motion still extracted from a longer generative sequence.',                                          NULL,   NULL,   NULL,  NULL,   410.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -13, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(16, 6, 'Salt Fields',          'Photography', 'Aerial drone series over evaporation pans at the Camargue.',                                           60.00,  40.00,  NULL,  'CM',   790.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -14, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(17, 5, 'Coastal Memory',       'Acrylic',     'Layered washes built up over a winter spent near the Atlantic.',                                       90.00, 120.00,  NULL,  'CM',  1880.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -16, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(18, 3, 'Paper Architecture',   'Mixed Media', 'Hand-cut paper assemblage referencing modernist housing blocks.',                                      40.00,  50.00,   3.00, 'CM',   540.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -18, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(19, 2, 'Hollow Bone',          'Sculpture',   'Cast in hydrocal, sanded and waxed to a translucent finish.',                                          25.00,  35.00,  20.00, 'CM',  2400.00, 'WIP',      'ORIGINAL',  DATEADD('DAY', -19, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(20, 6, 'Concrete Garden',      'Photography', 'Late-summer documentation of a brutalist housing courtyard.',                                          50.00,  70.00,  NULL,  'CM',   470.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -21, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(21, 4, 'Saturated Drift',      'Digital',     'High-saturation render iterating on a procedural noise field.',                                        NULL,   NULL,   NULL,  NULL,   330.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -22, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(22, 5, 'Slow Tide',            'Acrylic',     'Single-pour color study, painted standing up over six hours.',                                         80.00, 100.00,  NULL,  'CM',     NULL, 'WIP',      NULL,        DATEADD('DAY', -24, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(23, 1, 'The Inner Window',     'Oil',         'Interior with curtain, painted from a single window in winter.',                                       70.00,  90.00,  NULL,  'CM',  1320.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -26, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(24, 2, 'Mineral Studies',      'Sculpture',   'Series of small forms cast from collected riverbed pebbles.',                                          20.00,  15.00,  15.00, 'CM',   880.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -28, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(25, 3, 'Atlas of Quiet',       'Mixed Media', 'Bookwork: 64 pages of pressed botanicals and silver-leaf gilding.',                                    25.00,  30.00,   2.00, 'CM',  1100.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -29, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(26, 4, 'Phosphor Garden',      'Digital',     'Procedural growth study, slow-rendered as a still triptych.',                                          NULL,   NULL,   NULL,  NULL,   590.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -31, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(27, 6, 'Long Shadow',          'Photography', 'Late-afternoon study of a single concrete colonnade.',                                                 40.00,  60.00,  NULL,  'CM',   430.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -33, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(28, 5, 'Heatwave',             'Acrylic',     'High-key palette painted during the August heat dome.',                                                90.00, 110.00,  NULL,  'CM',   960.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -35, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(29, 1, 'Halftone Memoir',      'Oil',         'Portrait series translating found halftone scans into oil.',                                           50.00,  70.00,  NULL,  'CM',  1750.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -38, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(30, 2, 'Tide Marker',          'Sculpture',   'Outdoor piece sited at low tide, weathered by a full season.',                                        120.00, 200.00,  90.00, 'CM',  3200.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -40, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(31, 1, 'Vermillion Drift',     'Oil',         'Color-field study leaning into a single warm chord.',                                                 100.00, 120.00,  NULL,  'CM',  1280.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -42, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(32, 4, 'Quiet Shader',         'Digital',     'Slow caustics rendered against a matte interior plane.',                                               NULL,   NULL,   NULL,  NULL,   720.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -43, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(33, 6, 'Roof Geometry',        'Photography', 'Aerial study of a tile-roofed Mediterranean village.',                                                 50.00,  40.00,  NULL,  'CM',   510.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -45, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(34, 5, 'Quiet Surf',           'Acrylic',     'Cool-key wash painted from a single dawn observation.',                                                80.00, 100.00,  NULL,  'CM',  1620.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -46, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(35, 3, 'Cardinal Inventory',   'Mixed Media', 'Found-paper grid mapping color samples from a single block.',                                          50.00,  50.00,  NULL,  'CM',   680.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -48, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(36, 2, 'Bone Chair',           'Sculpture',   'Functional object cast in a single piece, finish-sanded by hand.',                                     45.00,  85.00,  50.00, 'CM',  5200.00, 'WIP',      'ORIGINAL',  DATEADD('DAY', -50, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(37, 1, 'Threshold IV',         'Oil',         'Doorway studies in a long suite, rendered in three sittings.',                                         60.00,  90.00,  NULL,  'CM',   990.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -52, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(38, 4, 'Halo Pass',            'Digital',     'Single render iterating volumetric fog around a hollow form.',                                         NULL,   NULL,   NULL,  NULL,   450.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -54, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(39, 6, 'Stair Section',        'Photography', 'Documentation of a single brutalist stairwell at noon.',                                               40.00,  60.00,  NULL,  'CM',   460.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -55, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(40, 5, 'Field Notes',          'Acrylic',     'Working notebook flattened into a single sheet of color.',                                             60.00,  80.00,  NULL,  'CM',     NULL, 'WIP',      NULL,        DATEADD('DAY', -57, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(41, 3, 'Pressed Light',        'Mixed Media', 'Cyanotype with botanical inclusions on archival rag.',                                                 30.00,  40.00,  NULL,  'CM',   420.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -59, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(42, 4, 'Lattice Bloom',        'Digital',     'Procedural growth resolving into a still flowering form.',                                             NULL,   NULL,   NULL,  NULL,   640.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -60, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(43, 1, 'Slow Window',          'Oil',         'Window study, late winter light over five sittings.',                                                  70.00,  90.00,  NULL,  'CM',  1150.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -62, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(44, 2, 'Stone Tongue',         'Sculpture',   'Chiselled basalt fragment mounted on a steel plate.',                                                  35.00,  25.00,  20.00, 'CM',  2750.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -64, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(45, 6, 'Civic Lattice',        'Photography', 'Repeating-window facade as a city portrait at dusk.',                                                  60.00,  80.00,  NULL,  'CM',   580.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -66, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(46, 5, 'Salt and Iron',        'Acrylic',     'Heavy-body pour over rust-treated linen.',                                                            100.00, 130.00,  NULL,  'CM',  1980.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -68, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(47, 3, 'Soft Index',           'Mixed Media', 'Encyclopedic page collage with hand-applied gouache.',                                                 40.00,  50.00,  NULL,  'CM',   490.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -70, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(48, 4, 'Tide Sequence',        'Digital',     'Three-frame still extracted from a longer water study.',                                               NULL,   NULL,   NULL,  NULL,   370.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -72, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(49, 6, 'Cold Plaza',           'Photography', 'Empty civic square photographed in deep winter morning.',                                              50.00,  70.00,  NULL,  'CM',   500.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -74, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(50, 1, 'Inner Garden',         'Oil',         'Domestic still life with a single citrus and a bowl.',                                                 50.00,  60.00,  NULL,  'CM',   860.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -76, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(51, 2, 'Salt Vessel',          'Sculpture',   'Hand-thrown vessel finished with a slow salt firing.',                                                 22.00,  30.00,  22.00, 'CM',   980.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -78, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(52, 5, 'Late Flood',           'Acrylic',     'Unprimed canvas absorbing a single low-key wash.',                                                     80.00, 110.00,  NULL,  'CM',  1340.00, 'WIP',      'ORIGINAL',  DATEADD('DAY', -80, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(53, 3, 'Quiet Type',           'Mixed Media', 'Letterpress proofs layered with translucent vellum.',                                                  30.00,  40.00,  NULL,  'CM',   330.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -82, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(54, 4, 'Warm Threshold',       'Digital',     'Renderpass tuned for a single warm threshold value.',                                                  NULL,   NULL,   NULL,  NULL,   410.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -84, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(55, 1, 'Open Score',           'Oil',         'Abstract panel painted to a single repeated rhythm.',                                                  90.00, 110.00,  NULL,  'CM',  1450.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -86, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(56, 6, 'Edge Case',            'Photography', 'Documentation of a single border between two materials.',                                              40.00,  50.00,  NULL,  'CM',   390.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -88, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(57, 2, 'Drift Anchor',         'Sculpture',   'Steel form intended to be sited near a moving shoreline.',                                            150.00, 220.00, 100.00, 'CM',  3850.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -90, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(58, 5, 'Heavy Air',            'Acrylic',     'Atmospheric study from a single humid afternoon.',                                                     80.00, 100.00,  NULL,  'CM',   920.00, 'FINISHED', 'AVAILABLE', DATEADD('DAY', -92, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(59, 3, 'Map of Hours',         'Mixed Media', 'Quilted fabric grid charting a year of one neighborhood.',                                             80.00,  80.00,  NULL,  'CM',   990.00, 'FINISHED', 'ORIGINAL',  DATEADD('DAY', -94, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(60, 4, 'Slow Network',         'Digital',     'Long render of a soft-graph topology, lit from below.',                                                NULL,   NULL,   NULL,  NULL,   560.00, 'FINISHED', 'EDITION',   DATEADD('DAY', -96, CURRENT_TIMESTAMP()),    CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO artwork_tags (artwork_id, tag) VALUES
(1, 'tokyo'), (1, 'neon'), (1, 'abstract'),
(2, 'sculpture'), (2, 'minimal'),
(3, 'botanical'), (3, 'layered'),
(4, 'render'), (4, '3d'),
(5, 'fluid'), (5, 'color'),
(6, 'brutalism'), (6, 'photo'),
(7, 'landscape'), (7, 'sunset'),
(8, 'geometry'), (8, 'pastel'),
(9, 'oil'), (9, 'warm'),
(10, 'cyanotype'), (10, 'botanical'),
(11, 'concrete'), (11, 'fog'),
(12, 'steel'), (12, 'suspended'),
(13, 'render'), (13, 'neon'),
(14, 'pour'), (14, 'fluid'),
(15, 'generative'), (15, 'twilight'),
(16, 'aerial'), (16, 'salt'),
(17, 'atlantic'), (17, 'wash'),
(18, 'paper'), (18, 'modernist'),
(19, 'hydrocal'), (19, 'translucent'),
(20, 'concrete'), (20, 'courtyard'),
(21, 'noise'), (21, 'render'),
(22, 'pour'), (22, 'study'),
(23, 'interior'), (23, 'window'),
(24, 'cast'), (24, 'mineral'),
(25, 'book'), (25, 'gilding'),
(26, 'procedural'), (26, 'triptych'),
(27, 'colonnade'), (27, 'shadow'),
(28, 'summer'), (28, 'palette'),
(29, 'portrait'), (29, 'halftone'),
(30, 'outdoor'), (30, 'weathered'),
(31, 'oil'), (31, 'color-field'),
(32, 'render'), (32, 'caustics'),
(33, 'aerial'), (33, 'roof'),
(34, 'dawn'), (34, 'wash'),
(35, 'paper'), (35, 'inventory'),
(36, 'cast'), (36, 'chair'),
(37, 'doorway'), (37, 'series'),
(38, 'fog'), (38, 'volumetric'),
(39, 'stair'), (39, 'concrete'),
(40, 'notebook'), (40, 'study'),
(41, 'cyanotype'), (41, 'pressed'),
(42, 'procedural'), (42, 'bloom'),
(43, 'window'), (43, 'winter'),
(44, 'basalt'), (44, 'mounted'),
(45, 'facade'), (45, 'civic'),
(46, 'rust'), (46, 'pour'),
(47, 'collage'), (47, 'gouache'),
(48, 'water'), (48, 'sequence'),
(49, 'plaza'), (49, 'winter'),
(50, 'still-life'), (50, 'citrus'),
(51, 'vessel'), (51, 'salt'),
(52, 'wash'), (52, 'low-key'),
(53, 'letterpress'), (53, 'vellum'),
(54, 'render'), (54, 'warm'),
(55, 'rhythm'), (55, 'panel'),
(56, 'border'), (56, 'edge'),
(57, 'shoreline'), (57, 'steel'),
(58, 'humid'), (58, 'atmosphere'),
(59, 'quilt'), (59, 'map'),
(60, 'topology'), (60, 'graph');

INSERT INTO artwork_image (id, artwork_id, image_url, sort_order, is_cover, caption, created_at) VALUES
-- Artwork 1 (multi-image example)
(1,  1,  'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(2,  1,  'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     1, FALSE, 'Detail crop',     CURRENT_TIMESTAMP()),
(3,  1,  'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        2, FALSE, 'Studio context',  CURRENT_TIMESTAMP()),
-- Artwork 2 (multi-image example)
(4,  2,  'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Main scan',       CURRENT_TIMESTAMP()),
(5,  2,  'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     1, FALSE, 'Side profile',    CURRENT_TIMESTAMP()),
-- Artwork 3..4
(6,  3,  'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Reference shot',  CURRENT_TIMESTAMP()),
(7,  4,  'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
-- Artwork 5 (multi-image example)
(8,  5,  'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(9,  5,  'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        1, FALSE, 'Surface detail',  CURRENT_TIMESTAMP()),
-- Artwork 6..11
(10, 6,  'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(11, 7,  'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(12, 8,  'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(13, 9,  'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(14, 10, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(15, 11, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
-- Artwork 12 (multi-image example)
(16, 12, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(17, 12, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     1, FALSE, 'Welds detail',    CURRENT_TIMESTAMP()),
(18, 12, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        2, FALSE, 'Install context', CURRENT_TIMESTAMP()),
-- Artwork 13..22
(19, 13, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(20, 14, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(21, 15, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(22, 16, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(23, 17, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(24, 18, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(25, 19, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(26, 20, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(27, 21, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(28, 22, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
-- Artwork 23 (multi-image example)
(29, 23, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(30, 23, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        1, FALSE, 'Window detail',   CURRENT_TIMESTAMP()),
-- Artwork 24..29
(31, 24, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(32, 25, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(33, 26, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(34, 27, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(35, 28, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(36, 29, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
-- Artwork 30 (multi-image example)
(37, 30, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(38, 30, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        1, FALSE, 'Sited at low tide', CURRENT_TIMESTAMP()),
(39, 30, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     2, FALSE, 'After season',    CURRENT_TIMESTAMP()),
-- Artwork 31..49
(40, 31, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(41, 32, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(42, 33, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(43, 34, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(44, 35, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(45, 36, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(46, 37, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(47, 38, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(48, 39, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(49, 40, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(50, 41, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(51, 42, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(52, 43, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(53, 44, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(54, 45, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(55, 46, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(56, 47, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(57, 48, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(58, 49, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
-- Artwork 50 (multi-image example)
(59, 50, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(60, 50, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        1, FALSE, 'Bowl detail',     CURRENT_TIMESTAMP()),
-- Artwork 51..60
(61, 51, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(62, 52, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(63, 53, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(64, 54, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(65, 55, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(66, 56, 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/960px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg', 0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(67, 57, 'https://i.etsystatic.com/5150206/r/il/a7ebd2/5329033525/il_fullxfull.5329033525_sg3b.jpg',                                                                          0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(68, 58, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYzoOMG7v3sdWE74CkRShXeu3VLBuUGRcu7g&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(69, 59, 'https://www.publicdomainpictures.net/pictures/240000/nahled/color-gradient-background.jpg',                                                                        0, TRUE,  'Cover image',     CURRENT_TIMESTAMP()),
(70, 60, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRXeoCVAfeolMJiXQkv8U1H8Gxn6cRbSYsiMQ&s',                                                                     0, TRUE,  'Cover image',     CURRENT_TIMESTAMP());

INSERT INTO comment (id, artwork_id, author_id, text, parent_comment_id, created_at, updated_at, is_deleted) VALUES
(1, 1, 2, 'Prekrasan rad!',           NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(2, 1, 3, 'Nastavak serije?',         NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE),
(3, 2, 1, 'Odlican kontrast i linije.', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), FALSE);

INSERT INTO artwork_like (id, artwork_id, user_id, created_at) VALUES
(1,  1,  2, CURRENT_TIMESTAMP()),
(2,  1,  3, CURRENT_TIMESTAMP()),
(3,  1,  4, CURRENT_TIMESTAMP()),
(4,  2,  1, CURRENT_TIMESTAMP()),
(5,  2,  3, CURRENT_TIMESTAMP()),
(6,  4,  1, CURRENT_TIMESTAMP()),
(7,  4,  5, CURRENT_TIMESTAMP()),
(8,  5,  1, CURRENT_TIMESTAMP()),
(9,  7,  6, CURRENT_TIMESTAMP()),
(10, 9,  3, CURRENT_TIMESTAMP()),
(11, 11, 1, CURRENT_TIMESTAMP()),
(12, 14, 4, CURRENT_TIMESTAMP()),
(13, 17, 6, CURRENT_TIMESTAMP()),
(14, 23, 2, CURRENT_TIMESTAMP()),
(15, 25, 1, CURRENT_TIMESTAMP());

INSERT INTO collection (id, owner_id, name, description, created_at, updated_at, is_public) VALUES
(1, 1, 'Urban Scenes',   'Street-focused sketches and photo references.',         DATEADD('DAY', -95, CURRENT_TIMESTAMP()), DATEADD('DAY', -95, CURRENT_TIMESTAMP()), TRUE),
(2, 1, 'Sunset Studies', 'Warm color palette experiments for landscape compositions.', DATEADD('DAY', -77, CURRENT_TIMESTAMP()), DATEADD('DAY', -77, CURRENT_TIMESTAMP()), FALSE),
(3, 2, 'Print Drafts',   'Mixed-media drafts prepared for print review.',         DATEADD('DAY', -40, CURRENT_TIMESTAMP()), DATEADD('DAY', -40, CURRENT_TIMESTAMP()), TRUE);

INSERT INTO collection_artwork (collection_id, artwork_id) VALUES
(1, 2),
(1, 3),
(2, 1),
(3, 3),
(3, 1);

INSERT INTO challenge (id, created_by, title, description, quote, kind, status, theme, cover_image_url, starts_at, ends_at, created_at, updated_at) VALUES
(1, 1, 'The Light of Dusk', 'Capturing the fleeting transition between day and night. We are looking for work that emphasizes long shadows, warm gradients, and the quiet melancholy of the blue hour.', 'Twilight is the crack between worlds where the light forgets its name and the shadows begin to sing.', 'FEATURED',         'ACTIVE', 'Dusk',       NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP()), DATEADD('DAY', 11, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
(2, 1, 'Brutalist Forms',   'A study of raw materials and uncompromising geometry. We celebrate the beauty of concrete, steel, and the rhythm of repetitive structures in the urban landscape.',     'The architect''s task is to make life more beautiful, but also to show the strength that holds it up.', 'COMMUNITY_CHOICE', 'ACTIVE', 'Brutalism',  NULL, DATEADD('DAY', -7, CURRENT_TIMESTAMP()), DATEADD('DAY',  7, CURRENT_TIMESTAMP()), CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

INSERT INTO challenge_submission (id, challenge_id, artwork_id, submitted_by, submitted_at) VALUES
(1,  1, 1,  1, DATEADD('HOUR',  -2, CURRENT_TIMESTAMP())),
(2,  1, 7,  1, DATEADD('HOUR',  -5, CURRENT_TIMESTAMP())),
(3,  1, 5,  5, DATEADD('HOUR',  -8, CURRENT_TIMESTAMP())),
(4,  1, 4,  4, DATEADD('HOUR', -12, CURRENT_TIMESTAMP())),
(5,  1, 3,  3, DATEADD('HOUR', -20, CURRENT_TIMESTAMP())),
(6,  1, 8,  4, DATEADD('HOUR', -26, CURRENT_TIMESTAMP())),
(7,  2, 6,  6, DATEADD('HOUR',  -1, CURRENT_TIMESTAMP())),
(8,  2, 2,  2, DATEADD('HOUR',  -6, CURRENT_TIMESTAMP())),
(9,  2, 4,  4, DATEADD('HOUR', -14, CURRENT_TIMESTAMP())),
(10, 2, 1,  1, DATEADD('HOUR', -22, CURRENT_TIMESTAMP())),
(11, 2, 5,  5, DATEADD('HOUR', -30, CURRENT_TIMESTAMP()));

ALTER TABLE app_user             ALTER COLUMN id RESTART WITH 100;
ALTER TABLE authority            ALTER COLUMN id RESTART WITH 100;
ALTER TABLE artwork              ALTER COLUMN id RESTART WITH 100;
ALTER TABLE artwork_image        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE comment              ALTER COLUMN id RESTART WITH 100;
ALTER TABLE artwork_like         ALTER COLUMN id RESTART WITH 100;
ALTER TABLE collection           ALTER COLUMN id RESTART WITH 100;
ALTER TABLE challenge            ALTER COLUMN id RESTART WITH 100;
ALTER TABLE challenge_submission ALTER COLUMN id RESTART WITH 100;
