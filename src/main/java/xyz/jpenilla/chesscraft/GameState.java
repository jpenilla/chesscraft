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
package xyz.jpenilla.chesscraft;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.Fen;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

public record GameState(
  UUID id,
  @Nullable UUID whiteId,
  int whiteElo,
  ChessGame.@Nullable TimeControl whiteTime,
  @Nullable UUID blackId,
  int blackElo,
  ChessGame.@Nullable TimeControl blackTime,
  List<ChessGame.Move> moves,
  Fen currentFen,
  int cpuMoveDelay,
  @Nullable Result result
) {
  public boolean whiteCpu() {
    return this.whiteId == null;
  }

  public boolean blackCpu() {
    return this.blackId == null;
  }

  public ChessPlayer white() {
    return this.whiteCpu()
      ? ChessPlayer.cpu(this.whiteElo())
      : ChessPlayer.player(Objects.requireNonNull(Bukkit.getPlayer(this.whiteId())));
  }

  public ChessPlayer black() {
    return this.blackCpu()
      ? ChessPlayer.cpu(this.blackElo())
      : ChessPlayer.player(Objects.requireNonNull(Bukkit.getPlayer(this.blackId())));
  }

  public boolean playersOnline() {
    try {
      this.black();
      this.white();
      return true;
    } catch (final NullPointerException e) {
      return false;
    }
  }

  public PieceColor color(final UUID playerId) {
    if (playerId.equals(this.whiteId)) {
      return PieceColor.WHITE;
    }
    return PieceColor.BLACK;
  }

  public UUID playerId(final PieceColor color) {
    if (color == PieceColor.WHITE) {
      return this.whiteId;
    } else {
      return this.blackId;
    }
  }

  public boolean cpu(final PieceColor color) {
    if (color == PieceColor.WHITE) {
      return this.whiteCpu();
    }
    return this.blackCpu();
  }

  public Component describe() {
    return text()
      .append(this.white().displayName().color(NamedTextColor.WHITE))
      .append(text(" vs "))
      .append(this.black().displayName().color(NamedTextColor.BLACK))
      .apply(builder -> {
        if (this.result != null) {
          builder.append(space())
            .append(text(this.result.toString()));
        }
      })
      .build();
  }

  public enum ResultType {
    WIN,
    STALEMATE,
    REPETITION,
    DRAW_BY_50,
    FORFEIT
  }

  public record Result(ResultType type, PieceColor color, long time) {
    static Result create(final ResultType type, final PieceColor color) {
      return new Result(type, color, System.currentTimeMillis());
    }
  }
}
