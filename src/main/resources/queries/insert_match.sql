REPLACE INTO chesscraft_matches (
  id,
  white_cpu,
  white_cpu_elo,
  white_player_id,
  white_time_control,
  black_cpu,
  black_cpu_elo,
  black_player_id,
  black_time_control,
  moves,
  current_fen,
  cpu_move_delay
) VALUES (
  :id,
  :white_cpu,
  :white_cpu_elo,
  :white_player_id,
  :white_time_control,
  :black_cpu,
  :black_cpu_elo,
  :black_player_id,
  :black_time_control,
  :moves,
  :current_fen,
  :cpu_move_delay
);