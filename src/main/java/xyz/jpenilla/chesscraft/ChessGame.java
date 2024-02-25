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
import com.destroystokyo.paper.util.SneakyThrow;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.Fen;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.display.AbstractTextDisplayHolder;
import xyz.jpenilla.chesscraft.display.BoardDisplaySettings;
import xyz.jpenilla.chesscraft.util.TimeUtil;
import xyz.jpenilla.chesscraft.util.Util;
import xyz.niflheim.stockfish.engine.QueryTypes;
import xyz.niflheim.stockfish.engine.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Option;
import xyz.niflheim.stockfish.exceptions.StockfishInitException;

public final class ChessGame implements BoardStateHolder {
  public static final NamespacedKey HIDE_LEGAL_MOVES_KEY = new NamespacedKey("chesscraft", "hide_legal_moves");

  private final UUID id;
  private final ChessBoard board;
  private final StockfishClient stockfish;
  // Rank 0-7 (8-1), File 0-7 (A-H)
  private final Piece[][] pieces;
  private final ChessCraft plugin;
  private final ChessPlayer white;
  private final ChessPlayer black;
  private final int moveDelay;
  private final CountDownLatch delayLatch = new CountDownLatch(1);
  private final TimeControlSettings timeControlSettings;
  private PieceType whiteNextPromotion = PieceType.QUEEN;
  private PieceType blackNextPromotion = PieceType.QUEEN;
  private final @Nullable TimeControl whiteTime;
  private final @Nullable TimeControl blackTime;
  private final @Nullable BukkitTask timeControlTask;
  private final List<Move> moves;
  private final List<Pair<BoardDisplaySettings<?>, ?>> displays = new ArrayList<>();
  private String currentFen;
  private PieceColor nextMove;
  private String selectedPiece;
  private Set<String> validDestinations;
  private CompletableFuture<?> activeQuery;
  private volatile boolean active = true;

  ChessGame(
    final ChessCraft plugin,
    final ChessBoard board,
    final ChessPlayer white,
    final ChessPlayer black,
    final @Nullable TimeControlSettings timeControl,
    final int moveDelay
  ) {
    this.id = UUID.randomUUID();
    this.plugin = plugin;
    this.board = board;
    this.white = white;
    this.black = black;
    this.pieces = ChessBoard.initBoard();
    this.moves = new CopyOnWriteArrayList<>();
    this.moveDelay = moveDelay;
    this.loadFen(Fen.STARTING_FEN);
    try {
      this.stockfish = this.createStockfishClient();
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to initialize and/or connect to chess engine process", ex);
    }
    if (timeControl != null) {
      this.whiteTime = new TimeControl(timeControl);
      this.blackTime = new TimeControl(timeControl);
      this.timeControlTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tickTime, 0L, 1L);
    } else {
      this.whiteTime = null;
      this.blackTime = null;
      this.timeControlTask = null;
    }
    this.timeControlSettings = timeControl;
    for (final BoardDisplaySettings<?> display : this.board.displays()) {
      this.displays.add(Pair.of(display, display.getOrCreateState(this.plugin, this.board)));
    }
    Util.scheduleOrRun(plugin, this::applyToWorld);
  }

  ChessGame(
    final ChessCraft plugin,
    final ChessBoard board,
    final GameState state
  ) {
    this.id = state.id();
    this.plugin = plugin;
    this.board = board;
    this.white = state.whiteCpu()
      ? ChessPlayer.cpu(state.whiteElo())
      : ChessPlayer.player(Objects.requireNonNull(Bukkit.getPlayer(state.whiteId())));
    this.black = state.blackCpu()
      ? ChessPlayer.cpu(state.blackElo())
      : ChessPlayer.player(Objects.requireNonNull(Bukkit.getPlayer(state.blackId())));
    this.pieces = ChessBoard.initBoard();
    this.moves = new CopyOnWriteArrayList<>(state.moves());
    this.moveDelay = state.cpuMoveDelay();
    this.loadFen(state.currentFen());
    try {
      this.stockfish = this.createStockfishClient();
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to initialize and/or connect to chess engine process", ex);
    }
    this.whiteTime = state.whiteTime() == null ? null : state.whiteTime().copy();
    this.blackTime = state.blackTime() == null ? null : state.blackTime().copy();
    if (this.whiteTime != null || this.blackTime != null) {
      this.timeControlTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tickTime, 0L, 1L);
    } else {
      this.timeControlTask = null;
    }
    this.timeControlSettings = state.timeControlSettings();
    for (final BoardDisplaySettings<?> display : this.board.displays()) {
      this.displays.add(Pair.of(display, display.getOrCreateState(this.plugin, this.board)));
    }
    Util.scheduleOrRun(plugin, this::applyToWorld);
  }

  public GameState snapshotState(final GameState.@Nullable Result result) {
    return new GameState(
      this.id,
      this.white instanceof ChessPlayer.Player player ? player.uuid() : null,
      this.white instanceof ChessPlayer.Cpu cpu ? cpu.elo() : -1,
      this.whiteTime == null ? null : this.whiteTime.copy(),
      this.black instanceof ChessPlayer.Player player ? player.uuid() : null,
      this.black instanceof ChessPlayer.Cpu cpu ? cpu.elo() : -1,
      this.blackTime == null ? null : this.blackTime.copy(),
      List.copyOf(this.moves),
      Fen.read(this.currentFen),
      this.moveDelay,
      this.timeControlSettings,
      result,
      null
    );
  }

  public ChessPlayer white() {
    return this.white;
  }

  public ChessPlayer black() {
    return this.black;
  }

  public boolean cpuVsCpu() {
    return this.white.isCpu() && this.black.isCpu();
  }

  public PieceColor nextMove() {
    return this.nextMove;
  }

  public void handleInteract(final Player player, final BoardPosition rightClicked) {
    final PieceColor color = this.color(ChessPlayer.player(player));
    if (color == null) {
      player.sendMessage(this.plugin.config().messages().notInThisGame());
      return;
    } else if (color != this.nextMove) {
      player.sendMessage(this.plugin.config().messages().notYourMove());
      return;
    } else if (this.activeQuery != null && !this.activeQuery.isDone()) {
      player.sendMessage(this.plugin.config().messages().chessEngineProcessing());
      return;
    }

    final String selNotation = rightClicked.notation();
    final Piece selPiece = this.piece(rightClicked);
    if (this.selectedPiece == null && selPiece != null) {
      if (selPiece.color() != color) {
        player.sendMessage(this.plugin.config().messages().notYourPiece());
        return;
      }
      this.activeQuery = this.selectPiece(selNotation);
    } else if (selNotation.equals(this.selectedPiece)) {
      this.selectedPiece = null;
      this.validDestinations = null;
    } else if (this.validDestinations != null && this.validDestinations.contains(selNotation)) {
      this.activeQuery = this.move(this.selectedPiece + selNotation, color).exceptionally(ex -> {
        this.plugin.getLogger().log(Level.WARNING, "Exception executing move", ex);
        return null;
      });
      this.validDestinations = null;
      this.selectedPiece = null;
    } else if (this.selectedPiece != null) {
      player.sendMessage(this.plugin.config().messages().invalidMove());
    }
  }

  public void displayParticles() {
    if (this.selectedPiece == null) {
      return;
    }

    final Vec3i selectedPos = this.board.toWorld(this.selectedPiece);

    final ChessPlayer c = this.player(this.nextMove);
    if (!(c instanceof ChessPlayer.OnlinePlayer chessPlayer)) {
      return;
    }
    final Player player = chessPlayer.player();
    this.blockParticles(player, selectedPos, Color.AQUA);

    if (this.validDestinations != null && !player.getPersistentDataContainer().has(HIDE_LEGAL_MOVES_KEY)) {
      this.validDestinations.stream()
        .map(this.board::toWorld)
        .forEach(pos -> this.blockParticles(player, pos, Color.GREEN));
    }
  }

  private void blockParticles(final Player player, final Vec3i block, final Color particleColor) {
    // 0.02 buffer around pieces
    final double min = 0.18D * this.board.scale();
    final double max = 0.82D * this.board.scale();
    final double minX = block.x() + min;
    final double minZ = block.z() + min;
    final double maxX = block.x() + max;
    final double maxZ = block.z() + max;

    boolean handledMaxCorner = false;
    for (double x = minX; x <= maxX; x += 0.2) {
      particle(player, particleColor, x, block.y(), minZ);
      particle(player, particleColor, x, block.y(), maxZ);
      if (x == maxX) {
        handledMaxCorner = true;
      }
    }

    // handle maximum corners if we didn't before (distance indivisible by increment)
    if (!handledMaxCorner) {
      particle(player, particleColor, maxX, block.y(), minZ);
      particle(player, particleColor, maxX, block.y(), maxZ);
    }

    // skip corners this time
    for (double z = minZ + 0.2; z <= maxZ - 0.2; z += 0.2) {
      particle(player, particleColor, minX, block.y(), z);
      particle(player, particleColor, maxX, block.y(), z);
    }
  }

  private static void particle(final Player player, final Color particleColor, final double x, final double y, final double z) {
    new ParticleBuilder(Particle.REDSTONE)
      .count(1)
      .color(particleColor)
      .offset(0, 0, 0)
      .location(player.getWorld(), x, y, z)
      .receivers(player)
      .spawn();
  }

  @Override
  public @Nullable Piece piece(final BoardPosition pos) {
    return this.pieces[pos.rank()][pos.file()];
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
    final ChessPlayer.Player chessPlayer = ChessPlayer.player(player);
    if (this.white instanceof ChessPlayer.Player p && p.uuid().equals(chessPlayer.uuid())) {
      return true;
    }
    return this.black instanceof ChessPlayer.Player p && p.uuid().equals(chessPlayer.uuid());
  }

  public void applyToWorld() {
    this.board.pieceHandler().applyToWorld(this.board, this, this.board.world());
    for (final Pair<BoardDisplaySettings<?>, ?> pair : this.displays) {
      if (pair.second() instanceof AbstractTextDisplayHolder t) {
        t.ensureSpawned();
        t.updateNow();
      }
    }
  }

  private CompletableFuture<Void> selectPiece(final String sel) {
    return this.stockfish.submit(QueryTypes.LEGAL_MOVES.builder(this.currentFen).build()).thenAccept(legal -> {
      this.selectedPiece = sel;
      this.validDestinations = legal.stream()
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
    return this.stockfish.submit(QueryTypes.LEGAL_MOVES.builder(this.currentFen).build()).thenCompose(validMoves -> {
      // engine will not make invalid moves, player moves are checked earlier
      if (validMoves.stream().noneMatch(valid -> valid.equals(move) || valid.startsWith(move))) {
        throw new IllegalArgumentException("Invalid move");
      }

      // Append promotion if needed
      final String finalMove = validMoves.contains(move) ? move : move + this.nextPromotionAndReset(color);

      final Move movePair = new Move(finalMove, color, null);
      if (!this.active) {
        // handle CPU running out of time control time
        return CompletableFuture.completedFuture(null);
      }
      return this.stockfish.submit(QueryTypes.MAKE_MOVES.builder(Fen.STARTING_FEN.fenString()).setMoves(this.moveSequenceString(movePair)).build()).thenCompose(newFen -> {
        final Fen fen = Fen.read(newFen);
        this.loadFen(fen);
        this.moves.add(movePair.boardAfter(fen));

        Util.schedule(this.plugin, this::applyToWorld);
        this.audience().sendMessage(this.plugin.config().messages().madeMove(
          this.player(color),
          this.player(color.other()),
          color,
          finalMove
        ));

        return this.checkForWinAfterMove();
      });
    }).thenCompose($ -> {
      if (this.player(this.nextMove).isCpu() && this.active) {
        return this.cpuMoveFuture();
      }
      return CompletableFuture.completedFuture(null);
    });
  }

  private String moveSequenceString(final Move... extraMoves) {
    return Stream.concat(this.moves.stream(), Arrays.stream(extraMoves)).map(Move::notation).collect(Collectors.joining(" "));
  }

  private String moveSequenceString() {
    return this.moves.stream().map(Move::notation).collect(Collectors.joining(" "));
  }

  private CompletableFuture<Void> checkForWinAfterMove() {
    // threefold repetition rule
    final Move lastMove = this.moves.get(this.moves.size() - 1);
    final long timesPositionSeen = this.moves.stream().filter(e -> Objects.deepEquals(e.boardAfter().pieces(), lastMove.boardAfter().pieces())).count();
    if (timesPositionSeen == 3) {
      this.announceDrawByRepetition();
      this.plugin.database().saveMatchAsync(
        this.snapshotState(GameState.Result.create(GameState.ResultType.REPETITION, null)),
        true
      );
      this.board.endGame();
      return CompletableFuture.completedFuture(null);
    }

    // fifty move rule
    if (this.moves.size() > 100) {
      final List<Move> last100 = Util.peekLast(this.moves, 100);
      boolean draw = true;
      Reference2IntMap<PieceType> lastCounts = null;
      Map<IntIntPair, Piece> lastPawns = null;
      for (final Move move : last100) {
        final Reference2IntMap<PieceType> newCounts = move.boardAfter().pieceTotals();
        if (lastCounts != null && !newCounts.equals(lastCounts)) {
          draw = false;
          break;
        }
        lastCounts = newCounts;

        final Map<IntIntPair, Piece> newPawns = move.boardAfter().pawnPositions();
        if (lastPawns != null && !newPawns.equals(lastPawns)) {
          draw = false;
          break;
        }
        lastPawns = newPawns;
      }

      if (draw) {
        this.announceDrawByFifty();
        this.plugin.database().saveMatchAsync(
          this.snapshotState(GameState.Result.create(GameState.ResultType.DRAW_BY_50, null)),
          true
        );
        this.board.endGame();
        return CompletableFuture.completedFuture(null);
      }
    }

    return this.stockfish.submit(QueryTypes.LEGAL_MOVES.builder(this.currentFen).build()).thenCompose(legal -> {
      if (!legal.isEmpty()) {
        final @Nullable TimeControl time = this.nextMove == PieceColor.WHITE ? this.blackTime : this.whiteTime;
        if (time != null) {
          time.move();
        }
        return CompletableFuture.completedFuture(null);
      }
      return this.stockfish.submit(QueryTypes.CHECKERS.builder(this.currentFen).build()).thenAccept(checkers -> {
        if (checkers.isEmpty()) {
          this.announceStalemate();
          this.plugin.database().saveMatchAsync(this.snapshotState(GameState.Result.create(GameState.ResultType.STALEMATE, null)), true);
        } else {
          this.announceWin(this.nextMove == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);
          this.plugin.database().saveMatchAsync(
            this.snapshotState(GameState.Result.create(GameState.ResultType.WIN, this.nextMove == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE)),
            true
          );
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
    this.audience().sendMessage(this.plugin.config().messages().cpuThinking(this.nextMove));
    return this.moveDelayFuture().thenCompose($ -> {
      if (!this.active) {
        // handle CPU running out of time control time
        return CompletableFuture.completedFuture(null);
      }
      return this.stockfish.submit(QueryTypes.BEST_MOVE.builder(Fen.STARTING_FEN.fenString())
          .setMoves(this.moveSequenceString())
          //.setDepth(10)
          .setMovetime(500L)
          .setUciElo(((ChessPlayer.Cpu) this.player(this.nextMove)).elo())
          .build())
        .thenCompose(bestMove -> {
          if (!this.active) {
            // handle CPU running out of time control time
            return CompletableFuture.completedFuture(null);
          }
          return this.move(bestMove, this.nextMove);
        });
    });
  }

  private CompletableFuture<Void> moveDelayFuture() {
    if (this.moveDelay == -1) {
      return CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.runAsync(() -> {
      try {
        this.delayLatch.await(this.moveDelay, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }

  public Audience audience() {
    final List<Audience> audiences = new ArrayList<>();
    audiences.add(this.white);
    audiences.add(this.black);
    for (final Pair<BoardDisplaySettings<?>, ?> pair : this.displays) {
      if (pair.second() instanceof Audience audience) {
        audiences.add(audience);
      }
    }
    return Audience.audience(audiences);
  }

  private void announceWin(final PieceColor winner) {
    this.audience().sendMessage(this.plugin.config().messages().checkmate(this.black, this.white, winner));
  }

  private void announceStalemate() {
    this.audience().sendMessage(this.plugin.config().messages().stalemate(this.black, this.white));
  }

  private void announceDrawByRepetition() {
    this.audience().sendMessage(this.plugin.config().messages().drawByRepetition(this.black, this.white));
  }

  private void announceDrawByFifty() {
    this.audience().sendMessage(this.plugin.config().messages().drawByFiftyMoveRule(this.black, this.white));
  }

  public void forfeit(final PieceColor color) {
    this.board.endGameAndWait();
    this.plugin.database().saveMatchAsync(
      this.snapshotState(GameState.Result.create(GameState.ResultType.FORFEIT, color)),
      true
    );
    this.audience().sendMessage(this.plugin.config().messages().forfeit(this.black, this.white, color));
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

  @SuppressWarnings("unchecked")
  public void close(final boolean removePieces, final boolean wait) {
    this.active = false;
    this.delayLatch.countDown();
    if (wait) {
      final @Nullable CompletableFuture<?> query = this.activeQuery;
      if (query != null) {
        try {
          query.get(5L, TimeUnit.SECONDS);
        } catch (final Throwable e) {
          SneakyThrow.sneaky(e);
        }
      }
    }
    if (removePieces) {
      this.board.pieceHandler().removeFromWorld(this.board, this.board.world());
    }
    if (this.timeControlTask != null) {
      this.timeControlTask.cancel();
    }
    for (final Pair<BoardDisplaySettings<?>, ?> pair : this.displays) {
      if (pair.first().removeAfterGame()) {
        ((BoardDisplaySettings<Object>) pair.first()).remove(pair.second());
      }
      ((BoardDisplaySettings<Object>) pair.first()).gameEnded(pair.second());
    }
    this.stockfish.close();
  }

  public void reset() {
    if (this.activeQuery != null && !this.activeQuery.isDone()) {
      throw new IllegalStateException();
    }
    this.activeQuery = this.stockfish.uciNewGame();
    this.activeQuery.join();
    this.loadFen(Fen.STARTING_FEN);
    this.applyToWorld();
  }

  private void loadFen(final Fen fen) {
    this.currentFen = fen.fenString();
    this.nextMove = fen.nextMove();
    System.arraycopy(fen.pieces(), 0, this.pieces, 0, fen.pieces().length);
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

  private StockfishClient createStockfishClient() throws StockfishInitException {
    final StockfishClient.Builder builder = new StockfishClient.Builder()
      .setPath(this.board.stockfishPath())
      .setOption(Option.Threads, 2);
    return builder.build();
  }

  private void tickTime() {
    Objects.requireNonNull(this.whiteTime, "whiteTime");
    Objects.requireNonNull(this.blackTime, "blackTime");
    if (this.nextMove == PieceColor.WHITE && this.whiteTime.tick()) {
      this.audience().sendMessage(this.plugin.config().messages().ranOutOfTime(this, PieceColor.WHITE));
      this.board.endGame();
    } else if (this.nextMove == PieceColor.BLACK && this.blackTime.tick()) {
      this.audience().sendMessage(this.plugin.config().messages().ranOutOfTime(this, PieceColor.BLACK));
      this.board.endGame();
    }
    if (this.plugin.getServer().getCurrentTick() % 4 == 0) {
      this.white.sendActionBar(this.plugin.config().messages().timeDisplay(this, PieceColor.WHITE));
      this.black.sendActionBar(this.plugin.config().messages().timeDisplay(this, PieceColor.BLACK));
    }
  }

  public TimeControl time(final ChessPlayer player) {
    if (player.equals(this.white)) {
      return this.whiteTime;
    } else if (player.equals(this.black)) {
      return this.blackTime;
    }
    throw new IllegalArgumentException();
  }

  public record Move(String notation, PieceColor color, @Nullable Fen boardAfter) {
    public Move boardAfter(final Fen fen) {
      return new Move(this.notation, this.color, fen);
    }

    public Fen boardAfter() {
      return Objects.requireNonNull(this.boardAfter);
    }
  }

  public static final class TimeControl {
    private final long increment;
    private volatile long timeLeft;

    TimeControl(final TimeControlSettings timeControl) {
      this.timeLeft = timeControl.time().toSeconds() * 20;
      this.increment = timeControl.increment().toSeconds() * 20;
    }

    public TimeControl(final long timeLeft, final long increment) {
      this.timeLeft = timeLeft;
      this.increment = increment;
    }

    synchronized void move() {
      this.timeLeft += this.increment;
    }

    synchronized boolean tick() {
      this.timeLeft--;
      return this.timeLeft < 1;
    }

    public String timeLeftString() {
      final Duration d = Duration.ofMillis(Math.round(this.timeLeft / 20.0D * 1000.0D));
      return TimeUtil.formatDurationClock(d);
    }

    public TimeControl copy() {
      return new TimeControl(this.timeLeft, this.increment);
    }
  }
}
