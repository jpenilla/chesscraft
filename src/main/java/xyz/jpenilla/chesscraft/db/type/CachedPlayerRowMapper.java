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
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.util.Util;

public final class CachedPlayerRowMapper implements RowMapper<ChessPlayer.CachedPlayer> {
  @Override
  public ChessPlayer.CachedPlayer map(final ResultSet rs, final StatementContext ctx) throws SQLException {
    final ColumnMapper<UUID> uuid = ctx.findColumnMapperFor(UUID.class).orElseThrow();
    final ColumnMapper<Component> component = ctx.findColumnMapperFor(Component.class).orElseThrow();
    return new ChessPlayer.CachedPlayer(
      uuid.map(rs, "id", ctx),
      Component.text(Util.trim(rs.getString("username"))),
      component.map(rs, "displayname", ctx),
      rs.getInt("rating"),
      rs.getInt("peak_rating"),
      rs.getInt("rated_matches")
    );
  }
}
