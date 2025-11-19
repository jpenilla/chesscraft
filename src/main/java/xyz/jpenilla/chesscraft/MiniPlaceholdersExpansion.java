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

import io.github.miniplaceholders.api.Expansion;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.type.tuple.Pair;
import xyz.jpenilla.chesscraft.util.MiniPlaceholdersUtil;

final class MiniPlaceholdersExpansion {
  private final ChessCraft plugin;

  public static void register(final ChessCraft plugin) {
    if (MiniPlaceholdersUtil.miniPlaceholdersLoaded()) {
      new MiniPlaceholdersExpansion(plugin).registerExpansion();
    }
  }

  private MiniPlaceholdersExpansion(final ChessCraft plugin) {
    this.plugin = plugin;
  }

  private void registerExpansion() {
    final Expansion expansion = Expansion.builder("chesscraft")
      .globalPlaceholder("leaderboard_uuid", (queue, ctx) -> {
        final Pair<UUID, Integer> entry = this.parseLeaderboardEntry(queue);
        return entry == null ? null : Tag.preProcessParsed(entry.first().toString());
      })
      .globalPlaceholder("leaderboard_elo", (queue, ctx) -> {
        final Pair<UUID, Integer> entry = this.parseLeaderboardEntry(queue);
        return entry == null ? null : Tag.preProcessParsed(Integer.toString(entry.second()));
      })
      .build();
    expansion.register();
  }

  private @Nullable Pair<UUID, Integer> parseLeaderboardEntry(final ArgumentQueue queue) {
    final int pos = queue.popOr("Missing position").asInt()
      .orElseThrow(() -> new IllegalArgumentException("Position must be an integer"));
    return this.plugin.database().getLeaderboardEntry(pos);
  }
}
