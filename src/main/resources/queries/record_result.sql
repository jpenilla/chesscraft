INSERT INTO chesscraft_complete_matches
(id,
 result_type,
 result_color,
 white_elo_change,
 black_elo_change)
VALUES (:id,
        :result_type,
        :result_color,
        :white_elo_change,
        :black_elo_change);
