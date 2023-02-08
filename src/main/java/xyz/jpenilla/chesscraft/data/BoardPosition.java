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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public record BoardPosition(int rank, int file) {
  private static final BiMap<Character, Integer> FILE_TO_INDEX = ImmutableBiMap.of(
    'a', 0,
    'b', 1,
    'c', 2,
    'd', 3,
    'e', 4,
    'f', 5,
    'g', 6,
    'h', 7
  );

  public static BoardPosition fromString(final String pos) {
    return new BoardPosition(
      8 - Integer.parseInt(String.valueOf(pos.charAt(1))),
      BoardPosition.FILE_TO_INDEX.get(pos.charAt(0))
    );
  }

  public String notation() {
    final char file = FILE_TO_INDEX.inverse().get(this.file);
    return String.valueOf(file) + (8 - this.rank);
  }
}
