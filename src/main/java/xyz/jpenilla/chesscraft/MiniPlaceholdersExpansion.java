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
