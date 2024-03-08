UPDATE chesscraft_players
SET username      = :username,
    displayname   = :displayname,
    rating        = :rating,
    peak_rating   = :peak_rating,
    rated_matches = :rated_matches
WHERE id = :id;
