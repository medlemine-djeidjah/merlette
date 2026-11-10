-- Merlette — schéma (repris de docs/schema.sql)

CREATE EXTENSION IF NOT EXISTS unaccent;  -- recherche d'espèces insensible aux accents (#16)


CREATE TYPE user_role          AS ENUM ('USER', 'ADMIN');
CREATE TYPE observation_status AS ENUM ('NEEDS_ID', 'IDENTIFIED');
CREATE TYPE notification_type  AS ENUM ('NEW_PROPOSAL', 'NEW_COMMENT', 'OBSERVATION_IDENTIFIED');

-- Utilisateurs ---------------------------------------------------------------
CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'USER',
    avatar_url    VARCHAR(500),
    bio           VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Référentiel ----------------------------------------------------------------
CREATE TABLE categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE species (
    id              SERIAL PRIMARY KEY,
    common_name     VARCHAR(150) NOT NULL,
    scientific_name VARCHAR(150) NOT NULL UNIQUE,
    category_id     INT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT
);
CREATE INDEX idx_species_category ON species(category_id);
CREATE INDEX idx_species_common_name ON species(LOWER(common_name));

-- Observations ---------------------------------------------------------------
CREATE TABLE observations (
    id                    SERIAL PRIMARY KEY,
    user_id               INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id           INT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    title                 VARCHAR(120),
    description           TEXT,
    observed_at           DATE NOT NULL CHECK (observed_at <= CURRENT_DATE),
    location_name         VARCHAR(200),
    latitude              NUMERIC(9,6) CHECK (latitude  BETWEEN -90  AND 90),
    longitude             NUMERIC(9,6) CHECK (longitude BETWEEN -180 AND 180),
    status                observation_status NOT NULL DEFAULT 'NEEDS_ID',
    identified_species_id INT REFERENCES species(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- statut et espèce retenue doivent être cohérents
    CHECK ((status = 'IDENTIFIED') = (identified_species_id IS NOT NULL))
);
CREATE INDEX idx_obs_created  ON observations(created_at DESC);
CREATE INDEX idx_obs_category ON observations(category_id, status);
CREATE INDEX idx_obs_user     ON observations(user_id);

CREATE TABLE photos (
    id             SERIAL PRIMARY KEY,
    observation_id INT NOT NULL REFERENCES observations(id) ON DELETE CASCADE,
    url            VARCHAR(500) NOT NULL,
    position       SMALLINT NOT NULL DEFAULT 0 CHECK (position BETWEEN 0 AND 4),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (observation_id, position)
);

-- Identification -------------------------------------------------------------
CREATE TABLE proposals (
    id             SERIAL PRIMARY KEY,
    observation_id INT NOT NULL REFERENCES observations(id) ON DELETE CASCADE,
    species_id     INT NOT NULL REFERENCES species(id) ON DELETE RESTRICT,
    user_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (observation_id, species_id),   -- une espèce proposée une seule fois par observation
    UNIQUE (id, observation_id)            -- cible de la clé étrangère composite de votes
);

CREATE TABLE votes (
    id             SERIAL PRIMARY KEY,
    observation_id INT NOT NULL,
    proposal_id    INT NOT NULL,
    user_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (observation_id, user_id),      -- un seul vote par utilisateur et par observation
    -- garantit que la proposition appartient bien à la même observation
    FOREIGN KEY (proposal_id, observation_id)
        REFERENCES proposals(id, observation_id) ON DELETE CASCADE
);
CREATE INDEX idx_votes_proposal ON votes(proposal_id);

-- Discussion -----------------------------------------------------------------
CREATE TABLE comments (
    id             SERIAL PRIMARY KEY,
    observation_id INT NOT NULL REFERENCES observations(id) ON DELETE CASCADE,
    user_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content        VARCHAR(1000) NOT NULL CHECK (LENGTH(TRIM(content)) > 0),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comments_obs  ON comments(observation_id, created_at);
CREATE INDEX idx_comments_user ON comments(user_id);

-- Notifications --------------------------------------------------------------
CREATE TABLE notifications (
    id             SERIAL PRIMARY KEY,
    user_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,   -- destinataire
    actor_id       INT REFERENCES users(id) ON DELETE SET NULL,           -- auteur de l'action
    observation_id INT REFERENCES observations(id) ON DELETE CASCADE,
    type           notification_type NOT NULL,
    message        VARCHAR(300) NOT NULL,
    is_read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read, created_at DESC);

