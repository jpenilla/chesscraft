SELECT id,
       username,
       displayname
FROM chesscraft_players
WHERE id = :id;
