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
package xyz.jpenilla.chesscraft.data;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.db.Database;

public final class MatchExporter {
  private final GameState state;
  private final StringBuilder out;

  private MatchExporter(final GameState state) {
    this.state = state;
    this.out = new StringBuilder();
  }

  public static CompletableFuture<String> writePgn(final GameState state, final Database db) {
    return new MatchExporter(state).writePgn(db);
  }

  private CompletableFuture<String> writePgn(final Database db) {
    final CompletableFuture<ChessPlayer> whiteFuture = this.state.whiteOffline(db);
    final CompletableFuture<ChessPlayer> blackFuture = this.state.blackOffline(db);
    return CompletableFuture.allOf(whiteFuture, blackFuture).thenApply($ -> {
      final LocalDateTime lastUpdated = this.state.lastUpdated().toLocalDateTime();

      this.appendTag("Event", "ChessCraft Match")
        .appendTag("Site", "Minecraft")
        .appendTag("Date", String.format("%d.%02d.%d", lastUpdated.getYear(), lastUpdated.getMonthValue(), lastUpdated.getDayOfMonth()))
        .appendTag("Round", "1")
        .appendTag("White", PlainTextComponentSerializer.plainText().serialize(whiteFuture.join().name()))
        .appendTag("Black", PlainTextComponentSerializer.plainText().serialize(blackFuture.join().name()));

      final GameState.@Nullable Result result = this.state.result();
      final @Nullable String resultString;
      if (result != null) {
        resultString = switch (result.type()) {
          case WIN -> result.color() == PieceColor.WHITE ? "1-0" : "0-1";
          case STALEMATE, REPETITION, DRAW_BY_50 -> "1/2-1/2";
          case FORFEIT -> result.color() == PieceColor.WHITE ? "0-1" : "1-0";
        };
      } else {
        resultString = null;
        this.appendTag("Result", "*");
      }
      if (resultString != null) {
        this.appendTag("Result", resultString)
          .appendTag("Termination", result.toString());
      }

      if (this.state.whiteCpu()) {
        this.appendTag("WhiteElo", String.valueOf(this.state.whiteElo()));
      }
      if (this.state.blackCpu()) {
        this.appendTag("BlackElo", String.valueOf(this.state.blackElo()));
      }

      // should be start time but is last updated, oh well (same for date)
      this.appendTag("Time", String.format("%02d:%02d:%02d", lastUpdated.getHour(), lastUpdated.getMinute(), lastUpdated.getSecond()));

      this.out.append('\n');

      // This doesn't use the most specific notation possible for moves, however it's enough
      // for chess.com and a couple other parsers I tested to work.
      for (int i = 0; i < this.state.moves().size(); i++) {
        final ChessGame.Move move = this.state.moves().get(i);
        final Piece movedPiece = move.boardAfter().piece(BoardPosition.fromString(move.notation().substring(2)));
        final String prefix = movedPiece.type() == PieceType.PAWN ? "" : movedPiece.type().upper();
        this.out.append(i + 1).append(i % 2 == 0 ? ". " : "... ")
          .append(prefix)
          .append(move.notation().substring(0, 4));
        if (move.notation().length() == 5) {
          // promotion
          this.out.append('=').append(
            String.valueOf(move.notation().charAt(4)).toUpperCase(Locale.ROOT)
          );
        }
        this.out.append(' ');
      }

      if (resultString != null) {
        this.out.append(resultString);
      }

      return this.out.toString();
    });
  }

  private @This MatchExporter appendTag(final String name, final String value) {
    this.out.append("[").append(name).append(" \"").append(value).append("\"]").append('\n');
    return this;
  }
}
