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
package xyz.jpenilla.chesscraft.util;

import io.github.miniplaceholders.api.MiniPlaceholders;

import java.util.Objects;

public final class MiniPlaceholdersUtil {

  private static byte miniPlaceholdersLoaded = -1;

  private MiniPlaceholdersUtil() {
  }

  public static boolean miniPlaceholdersLoaded() {
    if (miniPlaceholdersLoaded == -1) {
      try {
        final String name = MiniPlaceholders.class.getName();
        Objects.requireNonNull(name);
        miniPlaceholdersLoaded = 1;
      } catch (final NoClassDefFoundError error) {
        miniPlaceholdersLoaded = 0;
      }
    }
    return miniPlaceholdersLoaded == 1;
  }
}
