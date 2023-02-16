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

import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.BoardStateHolder;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

public record Fen(String fenString, Piece[][] pieces, PieceColor nextMove) implements BoardStateHolder {
  public static final Fen STARTING_FEN = Fen.read("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

  @Override
  public @Nullable Piece piece(final BoardPosition pos) {
    return this.pieces[pos.rank()][pos.file()];
  }

  public static Fen read(final String fenString) {
    final Piece[][] pieces = ChessBoard.initBoard();
    final String[] arr = fenString.split(" ");
    final String positions = arr[0];
    final PieceColor nextMove = PieceColor.decode(arr[1]);
    final String[] ranks = positions.split("/");
    for (int rank = 0; rank < ranks.length; rank++) {
      final String rankString = ranks[rank];
      int file = 0;
      for (final char c : rankString.toCharArray()) {
        try {
          final int empty = Integer.parseInt(String.valueOf(c));
          for (int i = 0; i < empty; i++) {
            pieces[rank][file + i] = null;
          }
          file += empty;
        } catch (final NumberFormatException ex) {
          pieces[rank][file] = Piece.decode(String.valueOf(c));
          file++;
        }
      }
    }
    return new Fen(fenString, pieces, nextMove);
  }
}
