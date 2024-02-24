/*
 * chesscraft
 *
 * Copyright (c) 2023 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jpenilla.chesscraft.db.type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.data.Fen;

public final class GameStateRowMapper implements RowMapper<GameState> {
  @Override
  public GameState map(final ResultSet rs, final StatementContext ctx) throws SQLException {
    final ColumnMapper<UUID> uuid = ctx.findColumnMapperFor(UUID.class).orElseThrow();
    final ColumnMapper<ChessGame.TimeControl> tc = ctx.findColumnMapperFor(ChessGame.TimeControl.class).orElseThrow();
    final ColumnMapper<List<ChessGame.Move>> moveListMapper = ctx.findColumnMapperFor(new GenericType<List<ChessGame.Move>>() {}).orElseThrow();
    final ColumnMapper<GameState.Result> resultMapper = ctx.findColumnMapperFor(GameState.Result.class).orElseThrow();
    final ColumnMapper<Fen> fenMapper = ctx.findColumnMapperFor(Fen.class).orElseThrow();

    final boolean whiteCpu = rs.getBoolean("white_cpu");
    final boolean blackCpu = rs.getBoolean("black_cpu");

    GameState.Result result;
    try {
      result = resultMapper.map(rs, "result", ctx);
    } catch (final SQLException e) {
      result = null;
    }

    return new GameState(
      uuid.map(rs, "id", ctx),
      whiteCpu ? null : uuid.map(rs, "white_player_id", ctx),
      whiteCpu ? rs.getInt("white_cpu_elo") : -1,
      tc.map(rs, "white_time_control", ctx),
      blackCpu ? null : uuid.map(rs, "black_player_id", ctx),
      blackCpu ? rs.getInt("black_cpu_elo") : -1,
      tc.map(rs, "black_time_control", ctx),
      moveListMapper.map(rs, "moves", ctx),
      fenMapper.map(rs, "current_fen", ctx),
      rs.getInt("cpu_move_delay"),
      result
    );
  }
}
