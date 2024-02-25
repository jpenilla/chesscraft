CREATE TABLE chesscraft_players
(
  `id` UUID NOT NULL PRIMARY KEY,
  `username` VARCHAR(30) NOT NULl,
  `displayname` MEDIUMTEXT NOT NULL
);

CREATE TABLE chesscraft_matches
(
  `id` UUID NOT NULL PRIMARY KEY,
  `white_cpu` BOOLEAN NOT NULL,
  `white_cpu_elo` INTEGER,
  `white_player_id` UUID REFERENCES chesscraft_players (`id`),
  `white_time_control` TEXT,
  `black_cpu` BOOLEAN NOT NULL,
  `black_cpu_elo` INTEGER,
  `black_player_id` UUID REFERENCES chesscraft_players (`id`),
  `black_time_control` TEXT,
  `moves` LONGTEXT NOT NULL,
  `current_fen` VARCHAR(128) NOT NULL,
  `cpu_move_delay` INTEGER NOT NULL,
  `time_control_settings` VARCHAR(32),
  `last_updated` DATETIME NOT NULL DEFAULT NOW()
);

CREATE TABLE chesscraft_complete_matches
(
  `id` UUID NOT NULL PRIMARY KEY REFERENCES chesscraft_matches (`id`),
  `result` MEDIUMTEXT NOT NULL
);
