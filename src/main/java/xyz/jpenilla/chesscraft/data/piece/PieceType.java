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
package xyz.jpenilla.chesscraft.data.piece;

import java.util.Locale;

public enum PieceType {
  PAWN("p"),
  BISHOP("b"),
  KNIGHT("n"),
  ROOK("r"),
  QUEEN("q"),
  KING("k");

  private final String abv;
  private final String abvUpper;

  PieceType(final String abv) {
    this.abv = abv;
    this.abvUpper = abv.toUpperCase(Locale.ENGLISH);
  }

  public String lower() {
    return this.abv;
  }

  public String upper() {
    return this.abvUpper;
  }
}
