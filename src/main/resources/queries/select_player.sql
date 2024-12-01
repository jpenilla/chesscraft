SELECT id,
       username,
       displayname,
       rating,
       peak_rating,
       rated_matches
FROM chesscraft_players
WHERE id = :id;
