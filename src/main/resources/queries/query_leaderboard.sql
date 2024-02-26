SELECT id,
       username,
       rating,
       rated_matches
FROM chesscraft_players
WHERE rated_matches > 0
ORDER BY rating DESC, username
LIMIT :limit;
