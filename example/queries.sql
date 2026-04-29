-- ============================================================
-- Приклади SQL-запитів до БД гри "LOST"
-- ============================================================

-- 1. Отримати топ-5 гравців за рейтингом
SELECT p.username, l.score, l.level_completed, l.completion_time_sec
FROM leaderboard_entries l
JOIN players p ON l.player_id = p.id
ORDER BY l.score DESC
LIMIT 5;

-- 2. Знайти всі збереження конкретного гравця
SELECT gs.save_name, gs.current_level, gs.health, gs.sanity, gs.saved_at
FROM game_saves gs
JOIN players p ON gs.player_id = p.id
WHERE p.username = 'jack_shephard'
ORDER BY gs.saved_at DESC;

-- 3. Показати інвентар гравця
SELECT ii.item_name, ii.item_type, ii.quantity, ii.item_value
FROM inventory_items ii
JOIN players p ON ii.player_id = p.id
WHERE p.username = 'jack_shephard';

-- 4. Отримати всіх NPC на конкретному рівні з їхніми репліками
SELECT n.npc_name, n.npc_type, dl.line_order, dl.dialogue_text
FROM npcs n
JOIN dialogue_lines dl ON n.id = dl.npc_id
WHERE n.level_number = 1
ORDER BY n.npc_name, dl.line_order;

-- 5. Показати активні сесії з кількістю гравців (M:N запит)
SELECT gs.session_code, gs.status, gs.current_level,
       p_host.username AS host_name,
       COUNT(sp.id) AS player_count
FROM game_sessions gs
JOIN players p_host ON gs.host_player_id = p_host.id
LEFT JOIN session_players sp ON gs.id = sp.session_id
WHERE gs.status = 'ACTIVE'
GROUP BY gs.id, gs.session_code, gs.status, gs.current_level, p_host.username;

-- 6. Знайти гравців, які грали більш ніж в одній сесії (M:N)
SELECT p.username, COUNT(sp.session_id) AS sessions_count
FROM players p
JOIN session_players sp ON p.id = sp.player_id
GROUP BY p.id, p.username
HAVING COUNT(sp.session_id) > 1;

-- 7. Отримати досягнення гравця
SELECT pa.achievement_name, pa.description, pa.unlocked_at
FROM player_achievements pa
JOIN players p ON pa.player_id = p.id
WHERE p.username = 'jack_shephard'
ORDER BY pa.unlocked_at;

