package xyz.jpenilla.minecraftchess;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.niflheim.stockfish.engine.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Query;
import xyz.niflheim.stockfish.engine.enums.QueryType;
import xyz.niflheim.stockfish.engine.enums.Variant;
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
  private final JavaPlugin plugin;
  private final ChessPlayer white;
  private final ChessPlayer black;
  private PieceType whiteNextPromotion = PieceType.QUEEN;
  private PieceType blackNextPromotion = PieceType.QUEEN;
  private String currentFen;
  private PieceColor nextMove;
  private String selectedPiece = null;
  private Set<String> validDestinations = null;
  private CompletableFuture<?> activeQuery;

  ChessGame(
    final JavaPlugin plugin,
    final ChessBoard board,
    final ChessPlayer white,
    final ChessPlayer black
  ) {
    this.plugin = plugin;
    this.board = board;
    this.white = white;
    this.black = black;
    this.pieces = initBoard();
    this.loadFen(STARTING_FEN);
    try {
      this.stockfish = createStockfishClient();
    } catch (final StockfishInitException ex) {
      throw new RuntimeException(ex);
    }
    this.applyToWorld();
  }

  public boolean handleInteract(final Player player, final int x, final int y, final int z) {
    if (x <= this.board.loc().x() + 7 && x >= this.board.loc().x()
      && z >= this.board.loc().z() - 7 && z <= this.board.loc().z()
      && (y >= this.board.loc().y() - 1 && y <= this.board.loc().y() + 1)) {
      final PieceColor color = this.color(ChessPlayer.player(player));
      if (color == null) {
        player.sendRichMessage("<red>You are not a player in this game!");
        return true;
      } else if (color != this.nextMove) {
        player.sendRichMessage("<red>Not your move!");
        return true;
      } else if (this.busy(player)) {
        return true;
      }

      final int xx = x - this.board.loc().x();
      final int zz = this.board.loc().z() - z;
      final String sel = MAPPING.inverse().get(zz) + (8 - xx);
      final Piece selPiece = this.piece(sel);
      if (this.selectedPiece == null && selPiece != null) {
        if (selPiece.white() != (color == PieceColor.WHITE)) {
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
    return false;
  }

  public void displayParticles() {
    if (this.selectedPiece == null) {
      return;
    }

    final Vec3 selectedPos = this.loc(this.selectedPiece);
    final World world = this.world();
    this.blockParticles(world, selectedPos, Color.AQUA);

    if (this.validDestinations != null) {
      this.validDestinations.stream()
        .map(this::loc)
        .forEach(pos -> this.blockParticles(world, pos, Color.GREEN));
    }
  }

  private void blockParticles(final World world, final Vec3 block, final Color particleColor) {
    for (double dx = 0.2; dx <= 0.8; dx += 0.2) {
      for (double dz = 0.2; dz <= 0.8; dz += 0.2) {
        if (dx == 0.2 || dz == 0.2 || dx == 0.8 || dz == 0.8) {
          new ParticleBuilder(Particle.REDSTONE)
            .count(1)
            .color(particleColor)
            .offset(0, 0, 0)
            .location(world, block.x() + dx, block.y(), block.z() + dz)
            .receivers(30, 30, 30)
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

  public ChessPlayer player(final PieceColor color) {
    if (color == PieceColor.WHITE) {
      return this.white;
    } else {
      return this.black;
    }
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
    final World world = this.world();
    final PieceHandler handler = new ItemFramePieceHandler();
    handler.applyToWorld(this, world);
  }

  public void cpuMove(final CommandSender sender) {
    if (this.player(this.nextMove) != ChessPlayer.CPU) {
      sender.sendRichMessage("<red>Not CPUs turn.");
      return;
    }
    if (this.busy(sender)) {
      return;
    }
    this.activeQuery = this.stockfish.submit(new Query.Builder(QueryType.Best_Move, this.currentFen).setDepth(10).build())
      .thenCompose(bestMove -> this.move(bestMove, this.nextMove))
      .exceptionally(ex -> {
        this.plugin.getLogger().log(Level.WARNING, "Exception executing CPU move", ex);
        return null;
      });
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
    });
  }

  private CompletableFuture<Void> checkForWin() {
    // win: no legal moves & checkers is not empty
    return this.stockfish.submit(new Query.Builder(QueryType.Checkers, this.currentFen).build()).thenCompose(checkers -> {
      if (!checkers.isEmpty()) {
        return this.stockfish.submit(new Query.Builder(QueryType.Legal_Moves, this.currentFen).build()).thenAccept(legal -> {
          if (legal.isEmpty()) {
            this.announceWin(this.nextMove == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);
            this.board.endGame();
          }
        });
      }
      return CompletableFuture.completedFuture(null);
    });
  }

  public Audience players() {
    return Audience.audience(this.white, this.black);
  }

  private void announceWin(final PieceColor winner) {
    this.players().sendMessage(Component.text(winner + " wins!", NamedTextColor.GREEN));
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

  private boolean busy(final CommandSender sender) {
    if (this.activeQuery != null && !this.activeQuery.isDone()) {
      sender.sendRichMessage("<red>Chess engine is currently processing, please try again shortly");
      return true;
    }
    return false;
  }

  private World world() {
    return Objects.requireNonNull(Bukkit.getWorld(this.board.worldKey()), "World is not loaded");
  }

  public void close() {
    this.stockfish.close();
  }

  public void reset() {
    this.loadFen(STARTING_FEN);
    this.stockfish.uciNewGame().join();
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
      for (char c : row.toCharArray()) {
        try {
          final int x = Integer.parseInt(String.valueOf(c));
          for (int xx = 0; xx < x; xx++) {
            this.pieces[r][i + xx] = null;
          }
          i += x;
        } catch (final NumberFormatException ex) {
          final PieceType type = PieceType.type(String.valueOf(c));
          this.pieces[r][i] = new Piece(
            type,
            type.upper().equals(String.valueOf(c))
          );
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

  private static StockfishClient createStockfishClient() throws StockfishInitException {
    return new StockfishClient.Builder().setVariant(Variant.AVX2).build();
  }

  private interface PieceHandler {
    void applyToWorld(ChessGame game, World world);

    static void applyCheckerboard(final ChessGame game, final World world) {
      for (int dx = 0; dx < 8; dx++) {
        for (int dz = 0; dz < 8; dz++) {
          final Material material = (dx * 7 + dz) % 2 == 0 ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE;
          final int x = game.board.loc().x() + dx;
          final int z = game.board.loc().z() - dz;
          world.setType(x, game.board.loc().y() - 1, z, material);
        }
      }
    }
  }

  private static final class ItemFramePieceHandler implements PieceHandler {
    @Override
    public void applyToWorld(final ChessGame game, final World world) {
      PieceHandler.applyCheckerboard(game, world);
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = game.board.loc().x() + i;
          final int zz = game.board.loc().z() - x;
          final Piece piece = game.pieces[i][x];

          final Collection<Entity> frame = world.getNearbyEntities(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), 0.25, 0.25, 0.25, e -> e instanceof ItemFrame || e instanceof ArmorStand);
          for (final Entity entity : frame) {
            entity.remove();
          }

          if (piece == null) {
            continue;
          }
          world.spawn(new Location(world, xx, game.board.loc().y(), zz), ItemFrame.class, itemFrame -> {
            final ItemStack stack = new ItemStack(Material.PAPER);
            stack.editMeta(meta -> {
              final int add = piece.white() ? 7 : 1;
              meta.setCustomModelData(piece.type().ordinal() + add);
            });
            itemFrame.setRotation(piece.white() ? Rotation.CLOCKWISE : Rotation.COUNTER_CLOCKWISE);
            itemFrame.setItem(stack);
            itemFrame.setFacingDirection(BlockFace.UP);
            itemFrame.setInvulnerable(true);
            itemFrame.setVisible(false);
            itemFrame.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, game.board.name());
          });
          world.spawn(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), ArmorStand.class, stand -> {
            stand.setInvulnerable(true);
            stand.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, game.board.name());
            stand.setGravity(false);
            stand.setInvisible(true);
          });
        }
      }
    }
  }

  private static final class ArmorStandPieceHandler implements PieceHandler {
    @Override
    public void applyToWorld(final ChessGame game, final World world) {
      PieceHandler.applyCheckerboard(game, world);
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = game.board.loc().x() + i;
          final int zz = game.board.loc().z() - x;
          final Piece piece = game.pieces[i][x];

          final Collection<Entity> stand = world.getNearbyEntities(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), 0.25, 0.25, 0.25, e -> e instanceof ArmorStand);
          for (final Entity entity : stand) {
            entity.remove();
          }

          if (piece == null) {
            continue;
          }

          world.spawn(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), ArmorStand.class, armorStand -> {
            armorStand.setCustomNameVisible(true);
            armorStand.customName(Component.text(piece.type().name()));
            if (piece.white()) {
              armorStand.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            } else {
              armorStand.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            }
            armorStand.setRotation(90, 0);
            armorStand.setInvulnerable(true);
            armorStand.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, game.board.name());
            armorStand.setGravity(false);
          });
        }
      }
    }
  }

  private static final class SignPieceHandler implements PieceHandler {
    @Override
    public void applyToWorld(final ChessGame game, final World world) {
      PieceHandler.applyCheckerboard(game, world);
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = game.board.loc().x() + i;
          final int zz = game.board.loc().z() - x;
          final Piece piece = game.pieces[i][x];

          if (piece == null) {
            world.setType(xx, game.board.loc().y(), zz, Material.AIR);
          } else {
            world.setType(xx, game.board.loc().y(), zz, piece.white() ? Material.BIRCH_SIGN : Material.DARK_OAK_SIGN);
            final Sign blockState = (Sign) world.getBlockState(xx, game.board.loc().y(), zz);
            blockState.line(0, Component.text(piece.type().name()));
            if (!piece.white()) {
              blockState.setColor(DyeColor.WHITE);
            }
            blockState.update();
          }
        }
      }
    }
  }
}
