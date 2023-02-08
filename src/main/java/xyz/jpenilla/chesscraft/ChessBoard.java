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

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.Vec3;

public final class ChessBoard {
  private final Vec3 loc;
  private final String name;
  private final ChessCraft plugin;
  private final NamespacedKey worldKey;
  private final Path stockfishPath;
  private final PieceHandler pieceHandler;
  private @Nullable ChessGame game;

  public ChessBoard(
    final ChessCraft plugin,
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
    this.pieceHandler = plugin.config().pieces().createHandler();
  }

  PieceHandler pieceHandler() {
    return this.pieceHandler;
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

  public Vec3 loc(final BoardPosition boardPosition) {
    return new Vec3(
      this.loc.x() + boardPosition.rank(),
      this.loc.y(),
      this.loc.z() - boardPosition.file()
    );
  }

  public Vec3 loc(final String pos) {
    return this.loc(BoardPosition.fromString(pos));
  }

  public void forEachPosition(final Consumer<BoardPosition> consumer) {
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        consumer.accept(new BoardPosition(rank, file));
      }
    }
  }

  public boolean handleInteract(final Player player, final int x, final int y, final int z) {
    if (this.hasGame() && this.worldKey.equals(player.getWorld().getKey()) && this.contains(x, y, z)) {
      this.game().handleInteract(player, this.toBoard(x, z));
      return true;
    }
    return false;
  }

  private boolean contains(final int x, final int y, final int z) {
    return x <= this.loc.x() + 7 && x >= this.loc.x()
      && z >= this.loc.z() - 7 && z <= this.loc.z()
      && y >= this.loc.y() - 1 && y <= this.loc.y() + 1;
  }

  private BoardPosition toBoard(final int worldX, final int worldZ) {
    return new BoardPosition(worldX - this.loc.x(), this.loc.z() - worldZ);
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
    this.startGame(white, black, -1);
  }

  public void startGame(final ChessPlayer white, final ChessPlayer black, final int cpuElo) {
    if (this.game != null) {
      throw new IllegalStateException("Board is occupied");
    }
    this.game = new ChessGame(this.plugin, this, white, black, cpuElo);
    this.game.players().sendMessage(this.plugin.config().messages().matchStarted(this, white, black));
    if (white.isCpu()) {
      this.game.cpuMove();
    }
  }

  public void endGame() {
    this.endGame(false);
  }

  public void endGame(boolean removePieces) {
    if (this.game == null) {
      throw new IllegalStateException("No game to end");
    }
    this.game.close(removePieces);
    this.game = null;
  }

  public void applyCheckerboard(
    final Material black,
    final Material white,
    final @Nullable Material border
  ) {
    final World world = this.world();
    for (int dx = 0; dx < 8; dx++) {
      for (int dz = 0; dz < 8; dz++) {
        final Material material = (dx * 7 + dz) % 2 == 0 ? white : black;
        world.setType(this.loc.x() + dx, this.loc.y() - 1, this.loc.z() - dz, material);
      }
    }
    if (border == null) {
      return;
    }
    for (int dx = -1; dx <= 8; dx++) {
      for (int dz = -1; dz <= 8; dz++) {
        if (dx == -1 || dz == -1 || dx == 8 || dz == 8) {
          world.setType(this.loc.x() + dx, this.loc.y() - 1, this.loc.z() - dz, border);
        }
      }
    }
  }

  World world() {
    return Objects.requireNonNull(this.plugin.getServer().getWorld(this.worldKey), "World '" + this.worldKey + "' is not loaded");
  }
}
