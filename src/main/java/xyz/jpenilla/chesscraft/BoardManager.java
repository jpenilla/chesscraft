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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.config.ConfigHelper;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.PVPChallenge;
import xyz.jpenilla.chesscraft.data.Vec3;

public final class BoardManager implements Listener {
  static final NamespacedKey PIECE_KEY = new NamespacedKey("chesscraft", "chess_piece");
  private final ChessCraft plugin;
  private final Path stockfishPath;
  private final Path file;
  private final Map<String, ChessBoard> boards;
  private final Cache<UUID, PVPChallenge> challenges;

  private BukkitTask particleTask;

  public BoardManager(final ChessCraft plugin, final Path stockfishPath) {
    this.plugin = plugin;
    this.stockfishPath = stockfishPath;
    this.file = plugin.getDataFolder().toPath().resolve("boards.yml");
    this.boards = new HashMap<>();
    this.challenges = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();
    try {
      Files.createDirectories(this.file.getParent());
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Cache<UUID, PVPChallenge> challenges() {
    return this.challenges;
  }

  public Collection<ChessBoard> boards() {
    return this.boards.values();
  }

  public Collection<ChessBoard> activeBoards() {
    return this.boards().stream().filter(ChessBoard::hasGame).toList();
  }

  public void createBoard(final String name, final World world, final Vec3 pos, final CardinalDirection facing) {
    final ChessBoard board = new ChessBoard(this.plugin, name, pos, facing, world.getKey(), this.stockfishPath);
    this.boards.put(name, board);
    this.saveBoards();
  }

  public void reload() {
    this.close();
    this.load();
  }

  public void deleteBoard(final String board) {
    final ChessBoard remove = this.boards.remove(board);
    if (remove == null) {
      throw new IllegalArgumentException(board);
    }
    if (remove.hasGame()) {
      remove.endGame(true);
    } else {
      remove.pieceHandler().removeFromWorld(remove, remove.world());
    }
  }

  public ChessBoard board(final String name) {
    return this.boards.get(name);
  }

  public void load() {
    this.loadBoards();
    this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    this.particleTask = this.plugin.getServer().getScheduler().runTaskTimer(
      this.plugin,
      () -> this.activeBoards().forEach(board -> board.game().displayParticles()),
      0L,
      5L
    );
  }

  private void loadBoards() {
    final Map<String, BoardData> read = ConfigHelper.loadConfig(new TypeToken<Map<String, BoardData>>() {}, this.file, HashMap::new);
    read.forEach((key, data) -> this.boards.put(key, new ChessBoard(this.plugin, key, data.position, data.facing, data.dimension, this.stockfishPath)));
  }

  public void close() {
    this.particleTask.cancel();
    this.particleTask = null;
    HandlerList.unregisterAll(this);
    this.boards.forEach((name, board) -> {
      if (board.hasGame()) {
        board.endGame();
      }
    });
    this.saveBoards();
    this.boards.clear();
  }

  private void saveBoards() {
    final Map<String, BoardData> collect = this.boards.values().stream()
      .map(b -> Map.entry(b.name(), new BoardData(b.worldKey(), b.loc(), b.facing())))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    ConfigHelper.saveConfig(this.file, new TypeToken<>() {}, collect);
  }

  public boolean inGame(final Player player) {
    return this.boards.values().stream().anyMatch(board -> board.hasGame() && board.game().hasPlayer(player));
  }

  @EventHandler
  public void interact(final PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    final Block clicked = Objects.requireNonNull(event.getClickedBlock());
    for (final ChessBoard board : this.activeBoards()) {
      if (board.handleInteract(event.getPlayer(), clicked.getX(), clicked.getY(), clicked.getZ())) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void interact(final PlayerInteractAtEntityEvent event) {
    if (event.getRightClicked().getType() != EntityType.ARMOR_STAND) {
      return;
    }
    final Location loc = event.getRightClicked().getLocation();
    this.interact(event, loc, event.getPlayer());
  }

  private void interact(final Cancellable event, final Location loc, final Player player) {
    for (final ChessBoard board : this.activeBoards()) {
      if (board.handleInteract(player, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void quit(final PlayerQuitEvent event) {
    for (final ChessBoard board : this.activeBoards()) {
      if (board.game().hasPlayer(event.getPlayer())) {
        board.endGame();
      }
    }
    for (final PVPChallenge challenge : List.copyOf(this.challenges.asMap().values())) {
      if (challenge.challenger().equals(event.getPlayer()) || challenge.player().equals(event.getPlayer())) {
        this.challenges.invalidate(challenge.player().getUniqueId());
      }
    }
  }

  @EventHandler
  public void damage(final HangingBreakEvent event) {
    if (!event.getEntity().getPersistentDataContainer().has(PIECE_KEY)) {
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler
  public void rotate(final PlayerItemFrameChangeEvent event) {
    if (event.getAction() != PlayerItemFrameChangeEvent.ItemFrameChangeAction.ROTATE
      || !event.getItemFrame().getPersistentDataContainer().has(PIECE_KEY)) {
      return;
    }
    event.setCancelled(true);
    this.interact(event, event.getItemFrame().getLocation().toBlockLocation(), event.getPlayer());
  }

  @EventHandler
  public void damage(final EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof ArmorStand || event.getEntity() instanceof ItemFrame)) {
      return;
    }
    final @Nullable String data = event.getEntity().getPersistentDataContainer().get(PIECE_KEY, PersistentDataType.STRING);
    if (data == null) {
      return;
    }
    event.setCancelled(true);
  }

  @ConfigSerializable
  public static final class BoardData {
    private NamespacedKey dimension = NamespacedKey.minecraft("overworld");
    private Vec3 position = new Vec3(0, 0, 0);
    private CardinalDirection facing = CardinalDirection.NORTH;

    @SuppressWarnings("unused")
    BoardData() {
    }

    BoardData(final NamespacedKey dimension, final Vec3 position, final CardinalDirection facing) {
      this.dimension = dimension;
      this.position = position;
      this.facing = facing;
    }
  }
}
