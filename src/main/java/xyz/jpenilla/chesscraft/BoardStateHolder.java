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

import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.piece.Piece;

public interface BoardStateHolder {
  @Nullable Piece piece(final BoardPosition pos);

  static BoardStateHolder of(final Piece[][] pieces) {
    return new BoardStateHolder() {
      @Override
      public @Nullable Piece piece(final BoardPosition pos) {
        return pieces[pos.rank()][pos.file()];
      }
    };
  }
}
