REPLACE INTO chesscraft_players (
  id,
  username,
  displayname,
  rating,
  peak_rating,
  rated_matches
) VALUES (
  :id,
  :username,
  :displayname,
  :rating,
  :peak_rating,
  :rated_matches
);
