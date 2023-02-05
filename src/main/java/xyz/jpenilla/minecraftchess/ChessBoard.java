/*
 * minecraft-chess
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
package xyz.jpenilla.minecraftchess;

import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.minecraftchess.data.Vec3;

public final class ChessBoard {
  private final Vec3 loc;
  private final String name;
  private final MinecraftChess plugin;
  private final NamespacedKey worldKey;
  private final Path stockfishPath;
  private @Nullable ChessGame game;

  public ChessBoard(
    final MinecraftChess plugin,
    final String name,
    final Vec3 loc,
    final NamespacedKey world,
    final Path stockfishPath
  ) {
    this.plugin = plugin;
    this.name = name;
    this.loc = loc;
    this.worldKey = world;
    this.stockfishPath = stockfishPath;
  }

  Path stockfishPath() {
    return this.stockfishPath;
  }

  public String name() {
    return this.name;
  }

  public Vec3 loc() {
    return this.loc;
  }

  public NamespacedKey worldKey() {
    return this.worldKey;
  }

  public boolean hasGame() {
    return this.game != null;
  }

  public ChessGame game() {
    return Objects.requireNonNull(this.game, "No game active");
  }

  public void startGame(final ChessPlayer white, final ChessPlayer black) {
    if (this.game != null) {
      throw new IllegalStateException("Board is occupied");
    }
    this.game = new ChessGame(this.plugin, this, white, black);
  }

  public void endGame() {
    if (this.game == null) {
      throw new IllegalStateException("No game to end");
    }
    this.game.close();
    this.game = null;
  }

  public boolean handleInteract(final int x, final int y, final int z, final Player player) {
    if (this.game != null) {
      return this.game.handleInteract(player, x, y, z);
    }
    return false;
  }
}
