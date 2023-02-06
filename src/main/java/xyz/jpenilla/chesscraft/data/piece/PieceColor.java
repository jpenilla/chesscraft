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

import net.kyori.adventure.text.format.NamedTextColor;

public enum PieceColor {
  WHITE("w", NamedTextColor.WHITE),
  BLACK("b", NamedTextColor.DARK_GRAY); // BLACK is too dark

  private final String abbreviation;
  private final NamedTextColor textColor;

  PieceColor(final String abbreviation, final NamedTextColor textColor) {
    this.abbreviation = abbreviation;
    this.textColor = textColor;
  }

  public NamedTextColor textColor() {
    return this.textColor;
  }

  public static PieceColor decode(final String s) {
    for (final PieceColor value : values()) {
      if (value.abbreviation.equals(s)) {
        return value;
      }
    }
    throw new IllegalArgumentException(s);
  }

  public PieceColor other() {
    return this == WHITE ? BLACK : WHITE;
  }
}
