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

import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.Fen;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.display.BoardDisplaySettings;

public final class ChessBoard {
  // southwest corner pos
  private final Vec3i loc;
  private final CardinalDirection facing;
  private final BoardManager.BoardData.AutoCpuGameSettings autoCpuGame;
  private final int scale;
  private final String name;
  private final ChessCraft plugin;
  private final NamespacedKey worldKey;
  private final Path stockfishPath;
  private final PieceHandler pieceHandler;
  private final List<? extends BoardDisplaySettings<?>> displays;
  private @Nullable ChessGame game;

  public ChessBoard(
    final ChessCraft plugin,
    final String name,
    final Vec3i loc,
    final CardinalDirection facing,
    final int scale,
    final NamespacedKey world,
    final List<? extends BoardDisplaySettings<?>> displays,
    final Path stockfishPath,
    final BoardManager.BoardData.AutoCpuGameSettings autoCpuGame
  ) {
    this.plugin = plugin;
    this.name = name;
    this.loc = loc;
    this.facing = facing;
    if (scale < 1) {
      throw new IllegalArgumentException("Scale cannot be less than 1.");
    }
    this.scale = scale;
    this.worldKey = world;
    this.displays = displays;
    this.stockfishPath = stockfishPath;
    this.autoCpuGame = autoCpuGame;
    this.pieceHandler = plugin.config().pieces().createHandler();
  }

  public List<? extends BoardDisplaySettings<?>> displays() {
    return this.displays;
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

  public Vec3i loc() {
    return this.loc;
  }

  public CardinalDirection facing() {
    return this.facing;
  }

  public int scale() {
    return this.scale;
  }

  public BoardManager.BoardData.AutoCpuGameSettings autoCpuGame() {
    return this.autoCpuGame;
  }

  public Vec3i toWorld(final BoardPosition boardPosition) {
    return this.toWorld0(rotate(boardPosition, this.facing.radians()));
  }

  public Vec3d toWorld(final Vec3d boardPosition) {
    return this.toWorld0(rotate(boardPosition, this.facing.radians()));
  }

  private Vec3i toWorld0(final BoardPosition boardPosition) {
    return new Vec3i(
      this.loc.x() + boardPosition.file() * this.scale,
      this.loc.y(),
      this.loc.z() + (boardPosition.rank() - 7) * this.scale
    );
  }

  private Vec3d toWorld0(final Vec3d boardPosition) {
    return new Vec3d(
      this.loc.x() + boardPosition.x() * this.scale,
      this.loc.y(),
      this.loc.z() + (boardPosition.z() - 7) * this.scale
    );
  }

  private BoardPosition toBoard(final int worldX, final int worldZ) {
    final BoardPosition pos = this.toBoard0(worldX, worldZ);
    return rotate(pos, this.facing.negativeRadians());
  }

  private BoardPosition toBoard0(final int worldX, final int worldZ) {
    return new BoardPosition(
      (7 * this.scale + worldZ - this.loc.z()) / this.scale,
      (worldX - this.loc.x()) / this.scale
    );
  }

  private static BoardPosition rotate(final BoardPosition pos, final double angleRadians) {
    final IntIntPair rotated = rotatePoint(
      pos.file(),
      pos.rank(),
      3.5,
      3.5,
      angleRadians
    );
    return new BoardPosition(rotated.secondInt(), rotated.firstInt());
  }

  private static Vec3d rotate(final Vec3d pos, final double angleRadians) {
    return rotatePoint(
      pos.x(),
      pos.z(),
      3.5,
      3.5,
      angleRadians
    );
  }

  private static IntIntPair rotatePoint(final int x, final int z, final double centerX, final double centerZ, final double angleRadians) {
    final double cos = Math.cos(angleRadians);
    final double sin = Math.sin(angleRadians);
    final int nX = (int) Math.round((x - centerX) * cos - (z - centerZ) * sin + centerX);
    final int nZ = (int) Math.round((x - centerX) * sin + (z - centerZ) * cos + centerZ);
    return IntIntPair.of(nX, nZ);
  }

  private static Vec3d rotatePoint(final double x, final double z, final double centerX, final double centerZ, final double angleRadians) {
    final double cos = Math.cos(angleRadians);
    final double sin = Math.sin(angleRadians);
    final double nX = (x - centerX) * cos - (z - centerZ) * sin + centerX;
    final double nZ = (x - centerX) * sin + (z - centerZ) * cos + centerZ;
    return new Vec3d(nX, 0, nZ);
  }

  public Vec3i toWorld(final String notation) {
    return this.toWorld(BoardPosition.fromString(notation));
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
    return x <= this.loc.x() + 7 * this.scale + this.scale - 1 && x >= this.loc.x()
      && z >= this.loc.z() - 7 * this.scale && z <= this.loc.z() + this.scale - 1
      && y >= this.loc.y() - 1 && y <= this.loc.y() + this.scale * 2 - 1;
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

  public void startCpuGame(
    final int moveDelay,
    final int whiteElo,
    final int blackElo,
    final @Nullable TimeControlSettings timeControl
  ) {
    this.startGame(
      ChessPlayer.cpu(whiteElo),
      ChessPlayer.cpu(blackElo),
      timeControl,
      moveDelay
    );
  }

  public void startGame(
    final ChessPlayer white,
    final ChessPlayer black,
    final @Nullable TimeControlSettings timeControl
  ) {
    this.startGame(white, black, timeControl, -1);
  }

  public void startGame(
    final ChessPlayer white,
    final ChessPlayer black,
    final @Nullable TimeControlSettings timeControl,
    final int moveDelay
  ) {
    if (this.autoCpuGame.enabled && !this.autoCpuGame.allowPlayerUse && (!white.isCpu() || !black.isCpu())) {
      throw new IllegalStateException("This board is only for CPU games!");
    }

    if (this.game != null) {
      throw new IllegalStateException("Board is occupied");
    }
    this.game = new ChessGame(this.plugin, this, white, black, timeControl, moveDelay);
    this.game.audience().sendMessage(this.plugin.config().messages().matchStarted(this, white, black));
    if (white.isCpu()) {
      this.game.cpuMove();
    }
  }

  public void resumeGame(final GameState state) {
    if (!state.playersOnline()) {
      throw new IllegalStateException();
    }

    if (this.autoCpuGame.enabled && !this.autoCpuGame.allowPlayerUse && (!state.white().isCpu() || !state.black().isCpu())) {
      throw new IllegalStateException("This board is only for CPU games!");
    }

    if (this.game != null) {
      throw new IllegalStateException("Board is occupied");
    }
    this.game = new ChessGame(this.plugin, this, state);
    this.game.audience().sendMessage(this.plugin.config().messages().matchResumed(this, state.white(), state.black()));
    if (state.white().isCpu() && state.currentFen().nextMove() == PieceColor.WHITE) {
      this.game.cpuMove();
    }
  }

  public void endGame() {
    this.endGame(false, false);
  }

  public void endGameAndWait() {
    this.endGame(false, true);
  }

  public void endGame(final boolean removePieces, final boolean wait) {
    if (this.game == null) {
      throw new IllegalStateException("No game to end");
    }
    this.game.close(removePieces, wait);
    this.game = null;
  }

  public void applyCheckerboard(
    final Material black,
    final Material white,
    final @Nullable Material border
  ) {
    final World world = this.world();
    this.forEachPosition(pos -> {
      final Vec3i loc = this.toWorld(pos);
      final Material material = (pos.rank() * 7 + pos.file()) % 2 == 0 ? white : black;
      for (int i = 0; i < this.scale; i++) {
        for (int h = 0; h < this.scale; h++) {
          world.setType(loc.x() + i, loc.y() - 1, loc.z() + h, material);
        }
      }
    });
    if (border == null) {
      return;
    }

    this.applyBorder(world, border);
  }

  private void applyBorder(final World world, final Material border) {
    final int minX = this.loc.x() - 1;
    final int maxZ = this.loc.z() + this.scale;
    final int minZ = this.loc.z() - this.scale * 7 - 1;
    final int maxX = this.loc.x() + this.scale * 8;

    for (int x = minX; x <= maxX; x++) {
      world.setType(new Location(world, x, this.loc.y() - 1, minZ), border);
      world.setType(new Location(world, x, this.loc.y() - 1, maxZ), border);
    }
    for (int z = minZ; z <= maxZ; z++) {
      world.setType(new Location(world, minX, this.loc.y() - 1, z), border);
      world.setType(new Location(world, maxX, this.loc.y() - 1, z), border);
    }
  }

  public World world() {
    return Objects.requireNonNull(this.plugin.getServer().getWorld(this.worldKey), "World '" + this.worldKey + "' is not loaded");
  }

  public void reset(final boolean clear) {
    if (this.hasGame()) {
      throw new IllegalStateException("Can't reset with an active game");
    }
    if (clear) {
      this.pieceHandler.removeFromWorld(this, this.world());
    } else {
      this.pieceHandler.applyToWorld(this, Fen.STARTING_FEN, this.world());
    }
  }

  public static Piece[][] initBoard() {
    final Piece[][] board = new Piece[8][8];
    for (int rank = 0; rank < board.length; rank++) {
      board[rank] = new Piece[8];
    }
    return board;
  }
}
