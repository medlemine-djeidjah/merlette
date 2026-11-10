-- Données de référence (seed) ------------------------------------------------
INSERT INTO categories (name, slug) VALUES
    ('Plantes', 'plantes'),
    ('Oiseaux', 'oiseaux'),
    ('Insectes', 'insectes'),
    ('Mammifères', 'mammiferes'),
    ('Reptiles & amphibiens', 'reptiles-amphibiens'),
    ('Champignons', 'champignons'),
    ('Poissons', 'poissons'),
    ('Autres', 'autres');

INSERT INTO species (common_name, scientific_name, category_id) VALUES
    ('Mésange bleue',          'Cyanistes caeruleus',   (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Mésange charbonnière',   'Parus major',           (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Rouge-gorge familier',   'Erithacus rubecula',    (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Merle noir',             'Turdus merula',         (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Moineau domestique',     'Passer domesticus',     (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Pigeon ramier',          'Columba palumbus',      (SELECT id FROM categories WHERE slug = 'oiseaux')),
    ('Pâquerette',             'Bellis perennis',       (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Pissenlit',              'Taraxacum officinale',  (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Coquelicot',             'Papaver rhoeas',        (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Chêne pédonculé',        'Quercus robur',         (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Lierre grimpant',        'Hedera helix',          (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Ortie dioïque',          'Urtica dioica',         (SELECT id FROM categories WHERE slug = 'plantes')),
    ('Abeille domestique',     'Apis mellifera',        (SELECT id FROM categories WHERE slug = 'insectes')),
    ('Coccinelle à 7 points',  'Coccinella septempunctata', (SELECT id FROM categories WHERE slug = 'insectes')),
    ('Paon-du-jour',           'Aglais io',             (SELECT id FROM categories WHERE slug = 'insectes')),
    ('Bourdon terrestre',      'Bombus terrestris',     (SELECT id FROM categories WHERE slug = 'insectes')),
    ('Piéride du chou',        'Pieris brassicae',      (SELECT id FROM categories WHERE slug = 'insectes')),
    ('Écureuil roux',          'Sciurus vulgaris',      (SELECT id FROM categories WHERE slug = 'mammiferes')),
    ('Hérisson d''Europe',     'Erinaceus europaeus',   (SELECT id FROM categories WHERE slug = 'mammiferes')),
    ('Renard roux',            'Vulpes vulpes',         (SELECT id FROM categories WHERE slug = 'mammiferes')),
    ('Chevreuil européen',     'Capreolus capreolus',   (SELECT id FROM categories WHERE slug = 'mammiferes')),
    ('Lézard des murailles',   'Podarcis muralis',      (SELECT id FROM categories WHERE slug = 'reptiles-amphibiens')),
    ('Grenouille rousse',      'Rana temporaria',       (SELECT id FROM categories WHERE slug = 'reptiles-amphibiens')),
    ('Crapaud commun',         'Bufo bufo',             (SELECT id FROM categories WHERE slug = 'reptiles-amphibiens')),
    ('Amanite tue-mouches',    'Amanita muscaria',      (SELECT id FROM categories WHERE slug = 'champignons')),
    ('Cèpe de Bordeaux',       'Boletus edulis',        (SELECT id FROM categories WHERE slug = 'champignons')),
    ('Girolle',                'Cantharellus cibarius', (SELECT id FROM categories WHERE slug = 'champignons')),
    ('Truite commune',         'Salmo trutta',          (SELECT id FROM categories WHERE slug = 'poissons')),
    ('Carpe commune',          'Cyprinus carpio',       (SELECT id FROM categories WHERE slug = 'poissons')),
    ('Escargot de Bourgogne',  'Helix pomatia',         (SELECT id FROM categories WHERE slug = 'autres'));

-- Compte admin de démo — mot de passe : Admin1234 (hash bcrypt, coût 10)
INSERT INTO users (username, email, password_hash, role) VALUES
    ('admin', 'admin@merlette.local', '$2b$10$4Mywp.tX58Cb9oyRKaAa7eZkRPfsCMZg0R94uix3GltcgcjMSeAAu', 'ADMIN');

