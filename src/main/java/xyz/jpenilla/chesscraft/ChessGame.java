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

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.Vec3;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.niflheim.stockfish.engine.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Option;
import xyz.niflheim.stockfish.engine.enums.Query;
import xyz.niflheim.stockfish.engine.enums.QueryType;
import xyz.niflheim.stockfish.exceptions.StockfishInitException;

public final class ChessGame {
  private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
  private static final BiMap<String, Integer> MAPPING = HashBiMap.create(8);

  static {
    MAPPING.put("a", 0);
    MAPPING.put("b", 1);
    MAPPING.put("c", 2);
    MAPPING.put("d", 3);
    MAPPING.put("e", 4);
    MAPPING.put("f", 5);
    MAPPING.put("g", 6);
    MAPPING.put("h", 7);
  }

  private final ChessBoard board;
  private final StockfishClient stockfish;
  private final Piece[][] pieces;
  private final ChessCraft plugin;
  private final ChessPlayer white;
  private final ChessPlayer black;
  private PieceType whiteNextPromotion = PieceType.QUEEN;
  private PieceType blackNextPromotion = PieceType.QUEEN;
  private String currentFen;
  private PieceColor nextMove;
  private String selectedPiece;
  private Set<String> validDestinations;
  private CompletableFuture<?> activeQuery;

  ChessGame(
    final ChessCraft plugin,
    final ChessBoard board,
    final ChessPlayer white,
    final ChessPlayer black,
    final int cpuElo
  ) {
    this.plugin = plugin;
    this.board = board;
    this.white = white;
    this.black = black;
    this.pieces = initBoard();
    this.loadFen(STARTING_FEN);
    try {
      this.stockfish = this.createStockfishClient(cpuElo);
    } catch (final StockfishInitException ex) {
      throw new RuntimeException(ex);
    }
    this.applyToWorld();
  }

  Piece[][] pieces() {
    return this.pieces;
  }

  public ChessPlayer white() {
    return this.white;
  }

  public ChessPlayer black() {
    return this.black;
  }

  public boolean handleInteract(final Player player, final int x, final int y, final int z) {
    if (x > this.board.loc().x() + 7 || x < this.board.loc().x()
      || z < this.board.loc().z() - 7 || z > this.board.loc().z()
      || y < this.board.loc().y() - 1 || y > this.board.loc().y() + 1) {
      return false;
    }
    final PieceColor color = this.color(ChessPlayer.player(player));
    if (color == null) {
      player.sendRichMessage("<red>You are not a player in this game!");
      return true;
    } else if (color != this.nextMove) {
      player.sendRichMessage("<red>Not your move!");
      return true;
    } else if (this.activeQuery != null && !this.activeQuery.isDone()) {
      player.sendRichMessage("<red>Chess engine is currently processing, please try again shortly");
      return true;
    }

    final int xx = x - this.board.loc().x();
    final int zz = this.board.loc().z() - z;
    final String sel = MAPPING.inverse().get(zz) + (8 - xx);
    final Piece selPiece = this.piece(sel);
    if (this.selectedPiece == null && selPiece != null) {
      if (selPiece.color() != color) {
        player.sendRichMessage("<red>Not your piece!");
        return true;
      }
      this.activeQuery = this.selectPiece(sel);
    } else if (sel.equals(this.selectedPiece)) {
      this.selectedPiece = null;
      this.validDestinations = null;
    } else if (this.validDestinations != null && this.validDestinations.contains(sel)) {
      this.activeQuery = this.move(this.selectedPiece + sel, color).exceptionally(ex -> {
        this.plugin.getLogger().log(Level.WARNING, "Exception executing move", ex);
        return null;
      });
      this.validDestinations = null;
      this.selectedPiece = null;
    } else if (this.selectedPiece != null) {
      player.sendRichMessage("<red>Invalid move!");
    }
    return true;
  }

  public void displayParticles() {
    if (this.selectedPiece == null) {
      return;
    }

    final Vec3 selectedPos = this.loc(this.selectedPiece);

    final ChessPlayer c = this.player(this.nextMove);
    if (!(c instanceof ChessPlayer.Player chessPlayer)) {
      return;
    }
    final Player player = chessPlayer.player();
    this.blockParticles(player, selectedPos, Color.AQUA);

    if (this.validDestinations != null) {
      this.validDestinations.stream()
        .map(this::loc)
        .forEach(pos -> this.blockParticles(player, pos, Color.GREEN));
    }
  }

  private void blockParticles(final Player player, final Vec3 block, final Color particleColor) {
    for (double dx = 0.2; dx <= 0.8; dx += 0.2) {
      for (double dz = 0.2; dz <= 0.8; dz += 0.2) {
        if (dx == 0.2 || dz == 0.2 || dx == 0.8 || dz == 0.8) {
          new ParticleBuilder(Particle.REDSTONE)
            .count(1)
            .color(particleColor)
            .offset(0, 0, 0)
            .location(player.getWorld(), block.x() + dx, block.y(), block.z() + dz)
            .receivers(player)
            .spawn();
        }
      }
    }
  }

  private Vec3 loc(final String pos) {
    final int x = this.board.loc().x() + 8 - Integer.parseInt(String.valueOf(pos.charAt(1)));
    final int z = this.board.loc().z() - MAPPING.get(String.valueOf(pos.charAt(0)));
    return new Vec3(x, this.board.loc().y(), z);
  }

  private @Nullable Piece piece(final String pos) {
    final int i = 7 - (Integer.parseInt(String.valueOf(pos.charAt(1))) - 1);
    final int j = MAPPING.get(String.valueOf(pos.charAt(0)));
    return this.pieces[i][j];
  }

  private String pos(final int i, final int j) {
    final String l = MAPPING.inverse().get(j);
    return l + (8 - i);
  }

  public ChessPlayer player(final PieceColor color) {
    if (color == PieceColor.WHITE) {
      return this.white;
    } else if (color == PieceColor.BLACK) {
      return this.black;
    }
    throw new IllegalArgumentException();
  }

  public @Nullable PieceColor color(final ChessPlayer player) {
    if (player.equals(this.white)) {
      return PieceColor.WHITE;
    } else if (player.equals(this.black)) {
      return PieceColor.BLACK;
    }
    return null;
  }

  public boolean hasPlayer(final Player player) {
    final ChessPlayer w = ChessPlayer.player(player);
    return this.white.equals(w) || this.black.equals(w);
  }

  public void applyToWorld() {
    this.board.pieceHandler().applyToWorld(this.board, this, this.board.world());
  }

  private CompletableFuture<Void> selectPiece(final String sel) {
    return this.stockfish.submit(new Query.Builder(QueryType.Legal_Moves, this.currentFen).build()).thenAccept(valid -> {
      this.selectedPiece = sel;
      this.validDestinations = Arrays.stream(valid.split(" "))
        .filter(move -> move.startsWith(sel))
        .map(move -> move.substring(2, 4))
        .collect(Collectors.toSet());
    }).exceptionally(ex -> {
      this.plugin.getLogger().log(Level.WARNING, "Failed to query valid moves", ex);
      return null;
    });
  }

  private CompletableFuture<Void> move(final String move, final PieceColor color) {
    if (color != this.nextMove) {
      throw new IllegalArgumentException("Wrong move");
    }
    return this.stockfish.submit(new Query.Builder(QueryType.Legal_Moves, this.currentFen).build()).thenCompose(response -> {
      final Set<String> validMoves = new HashSet<>(Arrays.asList(response.split(" ")));
      // engine will not make invalid moves, player moves are checked earlier
      if (validMoves.stream().noneMatch(valid -> valid.equals(move) || valid.startsWith(move))) {
        throw new IllegalArgumentException("Invalid move");
      }

      // Append promotion if needed
      final String finalMove = validMoves.contains(move) ? move : move + this.nextPromotionAndReset(color);

      return this.stockfish.submit(new Query.Builder(QueryType.Make_Move, this.currentFen).setMove(finalMove).build()).thenCompose(newFen -> {
        this.loadFen(newFen);
        this.plugin.getServer().getScheduler().runTask(this.plugin, this::applyToWorld);

        return checkForWin();
      });
    }).thenCompose($ -> {
      if (this.player(this.nextMove).isCpu()) {
        return this.cpuMoveFuture();
      }
      return CompletableFuture.completedFuture(null);
    });
  }

  private CompletableFuture<Void> checkForWin() {
    return this.stockfish.submit(new Query.Builder(QueryType.Legal_Moves, this.currentFen).build()).thenCompose(legal -> {
      if (!legal.isEmpty()) {
        return CompletableFuture.completedFuture(null);
      }
      return this.stockfish.submit(new Query.Builder(QueryType.Checkers, this.currentFen).build()).thenAccept(checkers -> {
        if (checkers.isEmpty()) {
          this.announceStalemate();
        } else {
          this.announceWin(this.nextMove == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);
        }
        this.board.endGame();
      });
    });
  }

  void cpuMove() {
    if (this.activeQuery != null && !this.activeQuery.isDone() || !this.player(this.nextMove).isCpu()) {
      throw new IllegalStateException();
    }
    this.activeQuery = this.cpuMoveFuture().exceptionally(ex -> {
      this.plugin.getLogger().log(Level.WARNING, "Exception executing move", ex);
      return null;
    });
  }

  private CompletableFuture<Void> cpuMoveFuture() {
    return this.stockfish.submit(new Query.Builder(QueryType.Best_Move, this.currentFen)
        //.setDepth(10)
        .setMovetime(1000L)
        .build())
      .thenCompose(bestMove -> this.move(bestMove, this.nextMove));
  }

  public Audience players() {
    return Audience.audience(this.white, this.black);
  }

  private void announceWin(final PieceColor winner) {
    this.players().sendMessage(this.plugin.config().messages().checkmate(this.black, this.white, winner));
  }

  private void announceStalemate() {
    this.players().sendMessage(this.plugin.config().messages().stalemate(this.black, this.white));
  }

  public void forfeit(final PieceColor color) {
    this.players().sendMessage(this.plugin.config().messages().forfeit(this.black, this.white, color));
    this.board.endGame();
  }

  private String nextPromotionAndReset(final PieceColor color) {
    switch (color) {
      case WHITE -> {
        final PieceType next = this.whiteNextPromotion;
        this.whiteNextPromotion = PieceType.QUEEN;
        return next.lower();
      }
      case BLACK -> {
        final PieceType next = this.blackNextPromotion;
        this.blackNextPromotion = PieceType.QUEEN;
        return next.lower();
      }
      default -> throw new IllegalArgumentException();
    }
  }

  public void close(final boolean removePieces) {
    if (removePieces) {
      this.board.pieceHandler().removeFromWorld(this.board, this.board.world());
    }
    this.stockfish.close();
  }

  public void reset() {
    if (this.activeQuery != null && !this.activeQuery.isDone()) {
      throw new IllegalStateException();
    }
    this.activeQuery = this.stockfish.uciNewGame();
    this.activeQuery.join();
    this.loadFen(STARTING_FEN);
    this.applyToWorld();
  }

  private void loadFen(final String fenString) {
    this.currentFen = fenString;
    final String[] arr = fenString.split(" ");
    final String positions = arr[0];
    this.nextMove = PieceColor.decode(arr[1]);
    final String[] rows = positions.split("/");
    for (int r = 0; r < rows.length; r++) {
      final String row = rows[r];
      int i = 0;
      for (final char c : row.toCharArray()) {
        try {
          final int x = Integer.parseInt(String.valueOf(c));
          for (int xx = 0; xx < x; xx++) {
            this.pieces[r][i + xx] = null;
          }
          i += x;
        } catch (final NumberFormatException ex) {
          this.pieces[r][i] = Piece.decode(String.valueOf(c));
          i++;
        }
      }
    }
  }

  public void nextPromotion(final Player sender, final PieceType type) {
    final ChessPlayer player = ChessPlayer.player(sender);
    if (player.equals(this.white)) {
      this.whiteNextPromotion = type;
    } else if (player.equals(this.black)) {
      this.blackNextPromotion = type;
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static Piece[][] initBoard() {
    final Piece[][] board = new Piece[8][8];
    for (int i = 0; i < board.length; i++) {
      board[i] = new Piece[8];
    }
    return board;
  }

  private StockfishClient createStockfishClient(final int cpuElo) throws StockfishInitException {
    final StockfishClient.Builder builder = new StockfishClient.Builder()
      .setPath(this.board.stockfishPath())
      .setOption(Option.Threads, 2);
    if (cpuElo != -1) {
      builder.setOption(Option.UCI_LIMITSTRENGTH, true)
        .setOption(Option.UCI_ELO, cpuElo);
    }
    return builder.build();
  }
}
