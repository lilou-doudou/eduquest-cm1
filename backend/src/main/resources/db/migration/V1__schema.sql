-- EduQuest CM1 — schéma initial (PostgreSQL 16)
-- Conventions : PK UUID, horodatages timestamptz (UTC), contraintes en base.

-- ---------------------------------------------------------------- auth / famille
CREATE TABLE parent_account (
    id                     uuid PRIMARY KEY,
    email_normalized       varchar(254) NOT NULL UNIQUE,
    password_hash          varchar(255) NOT NULL,
    email_verified_at      timestamptz,
    status                 varchar(16)  NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'DELETING')),
    created_at             timestamptz  NOT NULL,
    last_login_at          timestamptz,
    last_activity_at       timestamptz  NOT NULL,
    inactivity_notified_at timestamptz
);
CREATE INDEX idx_parent_activity ON parent_account (last_activity_at);

CREATE TABLE account_token (
    id         uuid PRIMARY KEY,
    parent_id  uuid        NOT NULL REFERENCES parent_account (id) ON DELETE CASCADE,
    type       varchar(20) NOT NULL CHECK (type IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL
);
CREATE INDEX idx_token_expires ON account_token (expires_at);
CREATE INDEX idx_token_parent ON account_token (parent_id);

CREATE TABLE child_profile (
    id          uuid PRIMARY KEY,
    parent_id   uuid        NOT NULL REFERENCES parent_account (id) ON DELETE CASCADE,
    nickname    varchar(30) NOT NULL CHECK (char_length(nickname) BETWEEN 2 AND 30),
    avatar_code varchar(40) NOT NULL,
    grade       varchar(8)  NOT NULL CHECK (grade = 'CM1'),
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL
);
CREATE INDEX idx_child_parent ON child_profile (parent_id);

CREATE TABLE privacy_acknowledgement (
    id                        uuid PRIMARY KEY,
    parent_id                 uuid        NOT NULL REFERENCES parent_account (id) ON DELETE CASCADE,
    child_id                  uuid REFERENCES child_profile (id) ON DELETE CASCADE,
    notice_version            varchar(32) NOT NULL,
    purpose_code              varchar(40) NOT NULL,
    legal_basis               varchar(40) NOT NULL,
    parent_action             varchar(40) NOT NULL,
    child_information_version varchar(32),
    recorded_at               timestamptz NOT NULL,
    withdrawn_at              timestamptz
);
CREATE INDEX idx_privacy_parent ON privacy_acknowledgement (parent_id);

-- ---------------------------------------------------------------- catalogue
CREATE TABLE world (
    id            uuid PRIMARY KEY,
    slug          varchar(40)  NOT NULL UNIQUE,
    title         varchar(80)  NOT NULL,
    subject_code  varchar(20)  NOT NULL UNIQUE,
    display_order int          NOT NULL UNIQUE CHECK (display_order > 0),
    availability  varchar(16)  NOT NULL CHECK (availability IN ('PLAYABLE', 'COMING_SOON')),
    asset_key     varchar(60)  NOT NULL,
    description   varchar(300) NOT NULL
);

CREATE TABLE level (
    id       uuid PRIMARY KEY,
    world_id uuid        NOT NULL REFERENCES world (id),
    ordinal  int         NOT NULL CHECK (ordinal > 0),
    title    varchar(80) NOT NULL,
    UNIQUE (world_id, ordinal)
);

CREATE TABLE mission (
    id       uuid PRIMARY KEY,
    level_id uuid        NOT NULL REFERENCES level (id),
    ordinal  int         NOT NULL CHECK (ordinal > 0),
    code     varchar(40) NOT NULL UNIQUE,
    UNIQUE (level_id, ordinal)
);

CREATE TABLE mission_version (
    id             uuid PRIMARY KEY,
    mission_id     uuid        NOT NULL REFERENCES mission (id),
    version_number int         NOT NULL CHECK (version_number > 0),
    title          varchar(80) NOT NULL,
    status         varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    content_hash   varchar(64) NOT NULL,
    published_at   timestamptz,
    UNIQUE (mission_id, version_number)
);
CREATE UNIQUE INDEX uq_mission_published ON mission_version (mission_id) WHERE status = 'PUBLISHED';

CREATE TABLE skill (
    id           uuid PRIMARY KEY,
    code         varchar(40)  NOT NULL UNIQUE,
    subject_code varchar(20)  NOT NULL,
    label        varchar(80)  NOT NULL,
    description  varchar(300) NOT NULL
);

CREATE TABLE exercise (
    id                 uuid PRIMARY KEY,
    mission_version_id uuid         NOT NULL REFERENCES mission_version (id),
    ordinal            int          NOT NULL CHECK (ordinal BETWEEN 1 AND 5),
    skill_id           uuid         NOT NULL REFERENCES skill (id),
    type               varchar(8)   NOT NULL CHECK (type IN ('MCQ', 'NUMBER', 'TEXT')),
    difficulty         int          NOT NULL CHECK (difficulty BETWEEN 1 AND 3),
    instruction        varchar(200) NOT NULL,
    prompt             varchar(400) NOT NULL,
    public_payload     jsonb        NOT NULL,
    answer_key         jsonb        NOT NULL,
    hint               varchar(300) NOT NULL,
    explanation        varchar(400) NOT NULL,
    UNIQUE (mission_version_id, ordinal)
);

-- ---------------------------------------------------------------- apprentissage
CREATE TABLE attempt (
    id                 uuid PRIMARY KEY,
    child_id           uuid        NOT NULL REFERENCES child_profile (id) ON DELETE CASCADE,
    mission_id         uuid        NOT NULL REFERENCES mission (id),
    mission_version_id uuid        NOT NULL REFERENCES mission_version (id),
    status             varchar(16) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    started_at         timestamptz NOT NULL,
    completed_at       timestamptz,
    correct_count      int CHECK (correct_count BETWEEN 0 AND 5),
    stars              int CHECK (stars BETWEEN 0 AND 3),
    completion_summary jsonb,
    version            bigint      NOT NULL DEFAULT 0
);
CREATE INDEX idx_attempt_child_date ON attempt (child_id, started_at DESC);
CREATE UNIQUE INDEX uq_attempt_in_progress ON attempt (child_id, mission_id) WHERE status = 'IN_PROGRESS';

CREATE TABLE attempt_answer (
    id                uuid PRIMARY KEY,
    attempt_id        uuid        NOT NULL REFERENCES attempt (id) ON DELETE CASCADE,
    exercise_id       uuid        NOT NULL REFERENCES exercise (id),
    submitted_answer  jsonb,
    is_correct        boolean,
    hint_used         boolean     NOT NULL DEFAULT false,
    hint_requested_at timestamptz,
    answered_at       timestamptz,
    UNIQUE (attempt_id, exercise_id),
    CHECK ((answered_at IS NULL) = (is_correct IS NULL))
);
CREATE INDEX idx_answer_attempt ON attempt_answer (attempt_id);

CREATE TABLE mission_progress (
    id                 uuid PRIMARY KEY,
    child_id           uuid NOT NULL REFERENCES child_profile (id) ON DELETE CASCADE,
    mission_id         uuid NOT NULL REFERENCES mission (id),
    best_correct_count int  NOT NULL CHECK (best_correct_count BETWEEN 0 AND 5),
    best_stars         int  NOT NULL CHECK (best_stars BETWEEN 0 AND 3),
    xp_awarded         int  NOT NULL CHECK (xp_awarded BETWEEN 0 AND 70),
    completed_attempts int  NOT NULL CHECK (completed_attempts >= 0),
    last_completed_at  timestamptz,
    UNIQUE (child_id, mission_id)
);
CREATE INDEX idx_progress_child ON mission_progress (child_id);

CREATE TABLE xp_event (
    id         uuid PRIMARY KEY,
    child_id   uuid        NOT NULL REFERENCES child_profile (id) ON DELETE CASCADE,
    attempt_id uuid        NOT NULL UNIQUE REFERENCES attempt (id) ON DELETE CASCADE,
    mission_id uuid        NOT NULL REFERENCES mission (id),
    amount     int         NOT NULL CHECK (amount > 0),
    created_at timestamptz NOT NULL
);
CREATE INDEX idx_xp_child ON xp_event (child_id);

-- ---------------------------------------------------------------- récompenses
CREATE TABLE reward_definition (
    id              uuid PRIMARY KEY,
    code            varchar(40)  NOT NULL UNIQUE,
    type            varchar(10)  NOT NULL CHECK (type IN ('BADGE', 'COSMETIC')),
    title           varchar(80)  NOT NULL,
    description     varchar(200) NOT NULL,
    asset_key       varchar(60)  NOT NULL,
    slot            varchar(16) CHECK (slot IN ('HEAD', 'BACK', 'COMPANION')),
    rule_type       varchar(30)  NOT NULL CHECK (rule_type IN
                        ('FIRST_STAR', 'FIRST_THREE_STARS', 'WORLD_COMPLETED', 'LEVEL_COMPLETED', 'RANK_REACHED')),
    rule_code       varchar(60)  NOT NULL UNIQUE,
    rule_parameters jsonb        NOT NULL,
    CHECK ((type = 'COSMETIC') = (slot IS NOT NULL))
);

CREATE TABLE child_reward (
    id                uuid PRIMARY KEY,
    child_id          uuid        NOT NULL REFERENCES child_profile (id) ON DELETE CASCADE,
    reward_id         uuid        NOT NULL REFERENCES reward_definition (id),
    granted_at        timestamptz NOT NULL,
    source_attempt_id uuid REFERENCES attempt (id) ON DELETE SET NULL,
    UNIQUE (child_id, reward_id)
);

CREATE TABLE child_equipment (
    child_id  uuid        NOT NULL REFERENCES child_profile (id) ON DELETE CASCADE,
    slot      varchar(16) NOT NULL CHECK (slot IN ('HEAD', 'BACK', 'COMPANION')),
    reward_id uuid        NOT NULL REFERENCES reward_definition (id),
    PRIMARY KEY (child_id, slot)
);

-- ---------------------------------------------------------------- exploitation
CREATE TABLE audit_event (
    id                     uuid PRIMARY KEY,
    event_type             varchar(40) NOT NULL,
    actor_pseudonymous_ref varchar(64),
    outcome                varchar(16) NOT NULL,
    created_at             timestamptz NOT NULL,
    correlation_id         varchar(64)
);
CREATE INDEX idx_audit_created ON audit_event (created_at);
CREATE INDEX idx_audit_actor ON audit_event (actor_pseudonymous_ref);

-- Limitation de débit persistante (fenêtres fixes)
CREATE TABLE rate_limit_bucket (
    bucket_key   varchar(200) NOT NULL,
    window_start timestamptz  NOT NULL,
    hits         int          NOT NULL,
    PRIMARY KEY (bucket_key, window_start)
);
CREATE INDEX idx_rate_limit_window ON rate_limit_bucket (window_start);

-- Registre minimal des suppressions, à réappliquer après une restauration de sauvegarde
CREATE TABLE deletion_record (
    id         uuid PRIMARY KEY,
    scope      varchar(16) NOT NULL CHECK (scope IN ('FAMILY', 'CHILD')),
    subject_id uuid        NOT NULL,
    deleted_at timestamptz NOT NULL
);
CREATE INDEX idx_deletion_date ON deletion_record (deleted_at);

-- ---------------------------------------------------------------- Spring Session JDBC
CREATE TABLE spring_session (
    primary_id            char(36)     NOT NULL,
    session_id            char(36)     NOT NULL,
    creation_time         bigint       NOT NULL,
    last_access_time      bigint       NOT NULL,
    max_inactive_interval int          NOT NULL,
    expiry_time           bigint       NOT NULL,
    principal_name        varchar(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id char(36)     NOT NULL,
    attribute_name     varchar(200) NOT NULL,
    attribute_bytes    bytea        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);
