SELECT chesscraft_matches.id,
       chesscraft_matches.white_cpu,
       chesscraft_matches.white_cpu_elo,
       chesscraft_matches.white_player_id,
       chesscraft_matches.white_time_control,
       chesscraft_matches.black_cpu,
       chesscraft_matches.black_cpu_elo,
       chesscraft_matches.black_player_id,
       chesscraft_matches.black_time_control,
       chesscraft_matches.moves,
       chesscraft_matches.current_fen,
       chesscraft_matches.cpu_move_delay,
       chesscraft_matches.time_control_settings,
       chesscraft_matches.last_updated,
       chesscraft_complete_matches.result_type,
       chesscraft_complete_matches.result_color
FROM chesscraft_matches
RIGHT OUTER JOIN chesscraft_complete_matches ON chesscraft_matches.id=chesscraft_complete_matches.id
WHERE white_player_id = :player_id OR black_player_id = :player_id;
