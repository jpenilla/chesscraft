SELECT chesscraft_matches.id AS id,
       chesscraft_matches.white_cpu AS white_cpu,
       chesscraft_matches.white_cpu_elo AS white_cpu_elo,
       chesscraft_matches.white_player_id AS white_player_id,
       chesscraft_matches.white_time_control AS white_time_control,
       chesscraft_matches.black_cpu AS black_cpu,
       chesscraft_matches.black_cpu_elo AS black_cpu_elo,
       chesscraft_matches.black_player_id AS black_player_id,
       chesscraft_matches.black_time_control AS black_time_control,
       chesscraft_matches.moves AS moves,
       chesscraft_matches.current_fen AS current_fen,
       chesscraft_matches.cpu_move_delay AS cpu_move_delay,
       chesscraft_matches.time_control_settings AS time_control_settings,
       chesscraft_matches.last_updated AS last_updated,
       chesscraft_complete_matches.result_type AS result_type,
       chesscraft_complete_matches.result_color AS result_color
FROM chesscraft_matches
LEFT OUTER JOIN chesscraft_complete_matches ON chesscraft_matches.id=chesscraft_complete_matches.id
WHERE chesscraft_matches.id = :id;
