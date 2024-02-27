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

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.config.Messages;
import xyz.jpenilla.chesscraft.data.Fen;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.db.Database;
import xyz.jpenilla.chesscraft.util.Elo;

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
  @Nullable TimeControlSettings timeControlSettings,
  @Nullable Result result,
  Timestamp lastUpdated
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

  public CompletableFuture<? extends ChessPlayer> whiteOffline(final Database db) {
    return this.whiteCpu()
      ? CompletableFuture.completedFuture(ChessPlayer.cpu(this.whiteElo()))
      : db.onlineOrCachedPlayer(this.whiteId());
  }

  public CompletableFuture<? extends ChessPlayer> blackOffline(final Database db) {
    return this.blackCpu()
      ? CompletableFuture.completedFuture(ChessPlayer.cpu(this.blackElo()))
      : db.onlineOrCachedPlayer(this.blackId());
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
    } else if (playerId.equals(this.blackId)) {
      return PieceColor.BLACK;
    }
    return null;
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

  public enum ResultType {
    WIN,
    STALEMATE,
    REPETITION,
    DRAW_BY_50,
    FORFEIT
  }

  public record Result(
    ResultType type,
    PieceColor color,
    int whiteEloChange,
    int blackEloChange
  ) {
    static Result create(final ResultType type, final PieceColor color) {
      return new Result(type, color, -1, -1);
    }

    public Component describe(final Messages messages) {
      return switch (this.type) {
        case WIN -> messages.resultWin(this.color);
        case STALEMATE -> messages.resultStalemate();
        case REPETITION -> messages.resultDrawByRepetition();
        case DRAW_BY_50 -> messages.resultDrawByFiftyMoveRule();
        case FORFEIT -> messages.resultForfeit(this.color);
      };
    }

    public boolean noRatingChange() {
      return this.whiteEloChange == 0 && this.blackEloChange == 0;
    }
  }

  public Elo.MatchOutcome matchOutcome() {
    final ResultType type = Objects.requireNonNull(this.result(), "result").type();
    if (type == ResultType.STALEMATE || type == ResultType.DRAW_BY_50 || type == ResultType.REPETITION) {
      return Elo.MatchOutcome.DRAW;
    } else if (type == ResultType.WIN) {
      return this.result().color() == PieceColor.WHITE ? Elo.MatchOutcome.PLAYER_ONE_WIN : Elo.MatchOutcome.PLAYER_TWO_WIN;
    } else { // FORFEIT
      return this.result().color() == PieceColor.WHITE ? Elo.MatchOutcome.PLAYER_TWO_WIN : Elo.MatchOutcome.PLAYER_ONE_WIN;
    }
  }
}
