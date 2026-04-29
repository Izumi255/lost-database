-- ============================================================
-- V1__create_schema.sql — Flyway Migration: Створення структури БД
-- СУБД: H2 (реляційна, вбудована)
-- ============================================================

-- ============================================================
-- Таблиця: players
-- Тип (за класифікацією): Довідкова (основна сутність)
-- Нормальна форма: 3НФ
--   1НФ: всі атрибути атомарні (немає масивів/множин)
--   2НФ: всі неключові атрибути залежать від повного PK (id)
--   3НФ: немає транзитивних залежностей між неключовими атрибутами
-- ============================================================
CREATE TABLE players (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(100),
    role            VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    total_score     INT DEFAULT 0,
    max_level_reached INT DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      TIMESTAMP
);

-- ============================================================
-- Таблиця: game_saves
-- Тип (за класифікацією): Робоча (оперативні дані)
-- Нормальна форма: 3НФ
--   1НФ: атомарні значення, є первинний ключ
--   2НФ: неключові атрибути повністю залежать від PK
--   3НФ: немає транзитивних залежностей
-- Зв'язок: players 1:N game_saves (один гравець — багато збережень)
-- ============================================================
CREATE TABLE game_saves (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id       BIGINT NOT NULL,
    current_level   INT NOT NULL,
    health          INT NOT NULL,
    max_health      INT DEFAULT 100,
    sanity          DOUBLE DEFAULT 100.0,
    position_x      DOUBLE NOT NULL,
    position_y      DOUBLE NOT NULL,
    save_name       VARCHAR(100),
    saved_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_save_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: inventory_items
-- Тип (за класифікацією): Робоча (оперативні дані)
-- Нормальна форма: 3НФ
--   1НФ: кожен предмет — окремий рядок, атомарні атрибути
--   2НФ: залежність від PK (id), не від частини складного ключа
--   3НФ: немає транзитивних залежностей
-- Зв'язок: players 1:N inventory_items
-- ============================================================
CREATE TABLE inventory_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id       BIGINT NOT NULL,
    item_type       VARCHAR(50) NOT NULL,
    item_name       VARCHAR(100) NOT NULL,
    quantity        INT DEFAULT 1,
    item_value         INT DEFAULT 0,
    CONSTRAINT fk_item_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: leaderboard_entries
-- Тип (за класифікацією): Робоча (оперативні дані)
-- Нормальна форма: 3НФ
--   1НФ: атомарні значення
--   2НФ: повна залежність від PK
--   3НФ: score, level_completed, completion_time_sec — незалежні
-- Зв'язок: players 1:N leaderboard_entries
-- ============================================================
CREATE TABLE leaderboard_entries (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id           BIGINT NOT NULL,
    score               INT NOT NULL,
    level_completed     INT NOT NULL,
    completion_time_sec DOUBLE,
    achieved_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leaderboard_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: player_achievements
-- Тип (за класифікацією): Робоча (оперативні дані)
-- Нормальна форма: 3НФ
--   1НФ: атомарні атрибути
--   2НФ: повна залежність від PK
--   3НФ: achievement_name не залежить від achievement_code
--        транзитивно (обидва описують одну сутність)
-- Зв'язок: players 1:N player_achievements
-- ============================================================
CREATE TABLE player_achievements (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id         BIGINT NOT NULL,
    achievement_code  VARCHAR(50) NOT NULL,
    achievement_name  VARCHAR(100) NOT NULL,
    description       VARCHAR(255),
    unlocked_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_achievement_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: npcs
-- Тип (за класифікацією): Довідкова (статичні ігрові дані)
-- Нормальна форма: 3НФ
--   1НФ: атомарні атрибути, є PK
--   2НФ: повна залежність від PK
--   3НФ: немає транзитивних залежностей
-- ============================================================
CREATE TABLE npcs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    npc_name        VARCHAR(50) NOT NULL,
    portrait_path   VARCHAR(255),
    sprite_path     VARCHAR(255),
    level_number    INT NOT NULL,
    spawn_x         DOUBLE NOT NULL,
    spawn_y         DOUBLE NOT NULL,
    npc_type        VARCHAR(30) NOT NULL
);

-- ============================================================
-- Таблиця: dialogue_lines
-- Тип (за класифікацією): Довідкова (контент/діалоги)
-- Нормальна форма: 3НФ
--   1НФ: атомарні значення
--   2НФ: повна залежність від PK
--   3НФ: немає транзитивних залежностей
-- Зв'язок: npcs 1:N dialogue_lines (один NPC — багато реплік)
-- ============================================================
CREATE TABLE dialogue_lines (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    npc_id            BIGINT NOT NULL,
    line_order        INT NOT NULL,
    speaker_name      VARCHAR(50) NOT NULL,
    portrait_key      VARCHAR(50),
    dialogue_text     TEXT NOT NULL,
    trigger_condition VARCHAR(100),
    CONSTRAINT fk_dialogue_npc FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: game_sessions
-- Тип (за класифікацією): Робоча (мультиплеєр сесії)
-- Нормальна форма: 3НФ
--   1НФ: атомарні значення, session_code — UNIQUE
--   2НФ: повна залежність від PK
--   3НФ: немає транзитивних залежностей
-- Зв'язок: players 1:N game_sessions (один гравець-хост)
-- ============================================================
CREATE TABLE game_sessions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_code      VARCHAR(10) NOT NULL UNIQUE,
    host_player_id    BIGINT NOT NULL,
    max_players       INT DEFAULT 4,
    status            VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    current_level     INT DEFAULT 1,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_host FOREIGN KEY (host_player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- ============================================================
-- Таблиця: session_players (зв'язувальна / асоціативна)
-- Тип (за класифікацією): Зв'язувальна (M:N між sessions і players)
-- Нормальна форма: 3НФ
--   1НФ: атомарні атрибути
--   2НФ: повна залежність від PK
--   3НФ: position_x/y, health — атрибути зв'язку, не транзитивні
-- Зв'язок: game_sessions M:N players (через цю таблицю)
--   game_sessions 1:N session_players
--   players 1:N session_players
-- ============================================================
CREATE TABLE session_players (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    player_id       BIGINT NOT NULL,
    position_x      DOUBLE DEFAULT 0,
    position_y      DOUBLE DEFAULT 0,
    health          INT DEFAULT 100,
    is_alive        BOOLEAN DEFAULT TRUE,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sp_session FOREIGN KEY (session_id) REFERENCES game_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- Індекси
CREATE INDEX idx_saves_player ON game_saves(player_id);
CREATE INDEX idx_inventory_player ON inventory_items(player_id);
CREATE INDEX idx_leaderboard_player ON leaderboard_entries(player_id);
CREATE INDEX idx_achievements_player ON player_achievements(player_id);
CREATE INDEX idx_dialogue_npc ON dialogue_lines(npc_id);
CREATE INDEX idx_sp_session ON session_players(session_id);
CREATE INDEX idx_sp_player ON session_players(player_id);

