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
