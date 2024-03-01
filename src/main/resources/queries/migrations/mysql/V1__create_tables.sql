CREATE TABLE chesscraft_players
(
  `id` UUID NOT NULL PRIMARY KEY,
  `username` VARCHAR(30) NOT NULl,
  `displayname` MEDIUMTEXT NOT NULL,
  `rating` INTEGER NOT NULL,
  `peak_rating` INTEGER NOT NULL,
  `rated_matches` INTEGER NOT NULL
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
  `time_control_settings` VARCHAR(128),
  `last_updated` DATETIME NOT NULL DEFAULT NOW()
);

CREATE TABLE chesscraft_complete_matches
(
  `id` UUID NOT NULL PRIMARY KEY REFERENCES chesscraft_matches (`id`),
  `result_type` VARCHAR(16) NOT NULL,
  `result_color` CHAR(1) CHECK(`result_color` = 'w' OR `result_color` = 'b'),
  `white_elo` INTEGER NOT NULL,
  `white_elo_change` INTEGER NOT NULL,
  `black_elo` INTEGER NOT NULL,
  `black_elo_change` INTEGER NOT NULL
);
