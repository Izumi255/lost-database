-- ============================================================
-- V2__seed_data.sql — Flyway Migration: Seeder (тестові дані)
-- ============================================================

-- 1. Гравці (players)
INSERT INTO players (username, password_hash, email, role, total_score, max_level_reached, created_at)
VALUES ('jack_shephard', '$2a$10$hashed_password_1', 'jack@oceanic.com', 'ROLE_ADMIN', 1500, 3, '2026-01-15 10:00:00');
INSERT INTO players (username, password_hash, email, role, total_score, max_level_reached, created_at)
VALUES ('kate_austen', '$2a$10$hashed_password_2', 'kate@oceanic.com', 'ROLE_USER', 1200, 3, '2026-01-16 12:30:00');
INSERT INTO players (username, password_hash, email, role, total_score, max_level_reached, created_at)
VALUES ('sayid_jarrah', '$2a$10$hashed_password_3', 'sayid@oceanic.com', 'ROLE_USER', 900, 2, '2026-01-17 08:45:00');
INSERT INTO players (username, password_hash, email, role, total_score, max_level_reached, created_at)
VALUES ('hugo_reyes', '$2a$10$hashed_password_4', 'hurley@oceanic.com', 'ROLE_USER', 600, 2, '2026-02-01 14:00:00');
INSERT INTO players (username, password_hash, email, role, total_score, max_level_reached, created_at)
VALUES ('john_locke', '$2a$10$hashed_password_5', 'locke@oceanic.com', 'ROLE_USER', 2000, 3, '2026-02-10 09:20:00');

-- 2. Збереження гри (game_saves)
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (1, 2, 85, 100, 72.5, 1200.0, 480.0, 'Before Boss', '2026-03-01 18:30:00');
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (1, 3, 60, 100, 45.0, 2400.0, 320.0, 'Final Level', '2026-03-05 20:15:00');
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (2, 1, 100, 100, 100.0, 64.0, 576.0, 'Start', '2026-03-02 10:00:00');
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (2, 2, 70, 100, 60.0, 900.0, 400.0, 'Mid Jungle', '2026-03-03 15:45:00');
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (3, 1, 90, 100, 88.0, 500.0, 576.0, 'Quick Save', '2026-03-04 11:20:00');
INSERT INTO game_saves (player_id, current_level, health, max_health, sanity, position_x, position_y, save_name, saved_at)
VALUES (5, 3, 40, 100, 20.0, 3200.0, 256.0, 'Almost Done', '2026-03-06 22:00:00');

-- 3. Предмети інвентарю (inventory_items)
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (1, 'weapon', 'Мачете', 1, 50);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (1, 'consumable', 'Аптечка', 3, 30);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (1, 'key', 'Ключ від бункера', 1, 100);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (2, 'weapon', 'Палка', 1, 20);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (2, 'consumable', 'Банан', 5, 10);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (3, 'consumable', 'Аптечка', 2, 30);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (4, 'consumable', 'Кокос', 8, 5);
INSERT INTO inventory_items (player_id, item_type, item_name, quantity, item_value) VALUES (5, 'weapon', 'Ніж', 1, 40);

-- 4. Таблиця лідерів (leaderboard_entries)
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (5, 2000, 3, 1845.5, '2026-03-06 22:30:00');
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (1, 1500, 3, 2100.0, '2026-03-05 21:00:00');
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (2, 1200, 2, 1500.0, '2026-03-03 16:00:00');
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (3, 900, 2, 1800.0, '2026-03-04 12:00:00');
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (4, 600, 1, 900.0, '2026-03-02 15:00:00');
INSERT INTO leaderboard_entries (player_id, score, level_completed, completion_time_sec, achieved_at) VALUES (1, 800, 2, 1350.0, '2026-03-01 19:00:00');

-- 5. Досягнення гравців (player_achievements)
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (1, 'FIRST_BLOOD', 'Перша кров', 'Знищити першого ворога', '2026-03-01 18:00:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (1, 'SURVIVOR', 'Вижити', 'Пройти рівень 1', '2026-03-01 18:30:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (1, 'COMPLETIONIST', 'Завершувач', 'Пройти всі 3 рівні', '2026-03-05 21:00:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (2, 'FIRST_BLOOD', 'Перша кров', 'Знищити першого ворога', '2026-03-02 10:30:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (2, 'SURVIVOR', 'Вижити', 'Пройти рівень 1', '2026-03-02 11:00:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (5, 'COMPLETIONIST', 'Завершувач', 'Пройти всі 3 рівні', '2026-03-06 22:30:00');
INSERT INTO player_achievements (player_id, achievement_code, achievement_name, description, unlocked_at) VALUES (5, 'SPEED_RUN', 'Спідран', 'Пройти рівень менш ніж за 5 хв', '2026-03-06 22:35:00');

-- 6. NPC (npcs)
INSERT INTO npcs (npc_name, portrait_path, sprite_path, level_number, spawn_x, spawn_y, npc_type) VALUES ('Кейт', '/images/kate_portrait.png', '/sprites/kate.png', 1, 640.0, 576.0, 'FRIENDLY');
INSERT INTO npcs (npc_name, portrait_path, sprite_path, level_number, spawn_x, spawn_y, npc_type) VALUES ('Саїд', '/images/sayid_portrait.png', '/sprites/sayid.png', 2, 320.0, 448.0, 'FRIENDLY');
INSERT INTO npcs (npc_name, portrait_path, sprite_path, level_number, spawn_x, spawn_y, npc_type) VALUES ('Бен', '/images/ben_portrait.png', '/sprites/ben.png', 3, 2800.0, 320.0, 'HOSTILE');
INSERT INTO npcs (npc_name, portrait_path, sprite_path, level_number, spawn_x, spawn_y, npc_type) VALUES ('Герлі', '/images/hurley_portrait.png', '/sprites/hurley.png', 1, 960.0, 576.0, 'FRIENDLY');

-- 7. Репліки діалогів (dialogue_lines)
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (1, 1, 'Кейт', 'kate', 'Гей, обережніше там! Дехто з тих, хто пішов за водою, так і не повернувся.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (1, 2, 'Кейт', 'kate', 'Кажуть, вони бачили... щось серед дерев.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (1, 3, 'Гравець', 'jack', 'Я мушу знайти кабіну пілотів, Кейт. Без трансивера ми тут назавжди.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (1, 4, 'Кейт', 'kate', 'Тримай очі відкритими. Якщо почуєш дивні звуки — краще біжи.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (2, 1, 'Саїд', 'sayid', 'Якщо ми не можемо викликати допомогу звідси, треба знайти вищу точку.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (2, 2, 'Саїд', 'sayid', 'Я бачив, що джунглі густішають на півночі. Можливо, там є пагорб.', NULL);
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (3, 1, 'Бен', 'ben', 'Ви не розумієте, де ви опинились. Цей острів — особливе місце.', 'LEVEL_3_ENTER');
INSERT INTO dialogue_lines (npc_id, line_order, speaker_name, portrait_key, dialogue_text, trigger_condition) VALUES (4, 1, 'Герлі', 'hurley', 'Чувак, ти бачив скільки бананів росте тут? Це ж рай!', NULL);

-- 8. Ігрові сесії (game_sessions)
INSERT INTO game_sessions (session_code, host_player_id, max_players, status, current_level, created_at) VALUES ('LOST01', 1, 4, 'ACTIVE', 2, '2026-03-10 19:00:00');
INSERT INTO game_sessions (session_code, host_player_id, max_players, status, current_level, created_at) VALUES ('LOST02', 5, 2, 'FINISHED', 3, '2026-03-08 20:00:00');
INSERT INTO game_sessions (session_code, host_player_id, max_players, status, current_level, created_at) VALUES ('LOST03', 2, 4, 'WAITING', 1, '2026-03-12 15:00:00');

-- 9. Гравці в сесіях (session_players) — M:N зв'язок
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (1, 1, 1200.0, 480.0, 85, TRUE, '2026-03-10 19:00:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (1, 2, 1100.0, 480.0, 70, TRUE, '2026-03-10 19:01:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (1, 3, 1050.0, 480.0, 90, TRUE, '2026-03-10 19:02:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (2, 5, 3200.0, 256.0, 40, TRUE, '2026-03-08 20:00:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (2, 4, 3100.0, 256.0, 0, FALSE, '2026-03-08 20:01:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (3, 2, 64.0, 576.0, 100, TRUE, '2026-03-12 15:00:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (3, 1, 64.0, 576.0, 100, TRUE, '2026-03-12 15:01:00');
INSERT INTO session_players (session_id, player_id, position_x, position_y, health, is_alive, joined_at) VALUES (3, 4, 64.0, 576.0, 100, TRUE, '2026-03-12 15:02:00');

