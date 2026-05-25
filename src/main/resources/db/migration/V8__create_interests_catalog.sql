-- =====================================================================
-- V8 — Interests catalogue + user picks (profile bounded context)
-- =====================================================================
-- `interests` is a curated catalogue (admin-managed in V2). Profiles pick
-- a subset via the `profile_interests` join. Slug is the natural key — stable
-- across renames and easy to consume from the front-end (i18n via label_fr / label_en).
--
-- Cross-module discipline: `profile_interests.profile_id` references profiles(id)
-- inside the same module (allowed). Future Phase G will move both tables into
-- the `profile` schema together — still no FK across modules.

CREATE TABLE interests (
    slug        VARCHAR(50)  PRIMARY KEY,
    label_fr    VARCHAR(100) NOT NULL,
    label_en    VARCHAR(100) NOT NULL,
    category    VARCHAR(30)  NOT NULL,
    icon        VARCHAR(50),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT ck_interests_category
        CHECK (category IN ('SPORT','CULTURE','TECH','FOOD','OUTDOOR'))
);

CREATE TABLE profile_interests (
    profile_id    UUID         NOT NULL REFERENCES profiles(id)  ON DELETE CASCADE,
    interest_slug VARCHAR(50)  NOT NULL REFERENCES interests(slug) ON DELETE RESTRICT,
    PRIMARY KEY (profile_id, interest_slug)
);

-- Reverse lookup "how many users picked this interest" (V2 recommendation / trending).
CREATE INDEX ix_profile_interests_slug ON profile_interests (interest_slug);

-- =====================================================================
-- Seed: ~15 starter interests with an Abidjan flavor. enabled=TRUE by default.
-- sort_order groups visually (10s = sport, 20s = culture, ...).
-- =====================================================================
INSERT INTO interests (slug, label_fr, label_en, category, icon, sort_order) VALUES
    ('foot',     'Football',       'Football',      'SPORT',   'soccer-ball',  10),
    ('basket',   'Basketball',     'Basketball',    'SPORT',   'basketball',   11),
    ('yoga',     'Yoga',           'Yoga',          'SPORT',   'yoga',         12),
    ('running',  'Running',        'Running',       'SPORT',   'shoe',         13),
    ('musique',  'Musique',        'Music',         'CULTURE', 'music',        20),
    ('cinema',   'Cinéma',         'Cinema',        'CULTURE', 'film',         21),
    ('art',      'Art & Expos',    'Art & Expos',   'CULTURE', 'palette',      22),
    ('lecture',  'Lecture',        'Reading',       'CULTURE', 'book',         23),
    ('tech',     'Tech & Code',    'Tech & Code',   'TECH',    'laptop',       30),
    ('photo',    'Photographie',   'Photography',   'TECH',    'camera',       31),
    ('cuisine',  'Cuisine',        'Cooking',       'FOOD',    'chef-hat',     40),
    ('resto',    'Restaurants',    'Restaurants',   'FOOD',    'fork-knife',   41),
    ('voyage',   'Voyage',         'Travel',        'OUTDOOR', 'plane',        50),
    ('plage',    'Plage',          'Beach',         'OUTDOOR', 'beach',        51),
    ('soiree',   'Soirées',        'Parties',       'OUTDOOR', 'party',        52);
