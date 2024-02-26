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
package xyz.jpenilla.chesscraft.db;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.flywaydb.core.Flyway;
import org.incendo.cloud.type.tuple.Pair;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.async.JdbiExecutor;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.config.DatabaseSettings;
import xyz.jpenilla.chesscraft.db.type.CachedPlayerRowMapper;
import xyz.jpenilla.chesscraft.db.type.ComponentColumnMapper;
import xyz.jpenilla.chesscraft.db.type.FenColumnMapper;
import xyz.jpenilla.chesscraft.db.type.GameStateRowMapper;
import xyz.jpenilla.chesscraft.db.type.MoveListColumnMapper;
import xyz.jpenilla.chesscraft.db.type.NativeUUIDColumnMapper;
import xyz.jpenilla.chesscraft.db.type.ResultColumnMapper;
import xyz.jpenilla.chesscraft.db.type.TimeControlColumnMapper;
import xyz.jpenilla.chesscraft.db.type.TimeControlSettingsColumnMapper;
import xyz.jpenilla.chesscraft.util.Elo;
import xyz.jpenilla.gremlin.runtime.util.Util;

public final class Database implements Listener {
  private final ChessCraft plugin;
  private final HikariDataSource dataSource;
  private final Jdbi jdbi;
  private final JdbiExecutor jdbiExecutor;
  private final ExecutorService threadPool;
  private final QueriesLocator queries;
  private final AsyncLoadingCache<UUID, ChessPlayer.CachedPlayer> playerCache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(15))
    .buildAsync((key, executor) -> Database.this.queryPlayer(key).thenApply(o -> o.orElse(null)).toCompletableFuture());

  private Database(final ChessCraft plugin, final HikariDataSource dataSource, final Jdbi jdbi) {
    this.plugin = plugin;
    this.dataSource = dataSource;
    this.jdbi = jdbi;
    this.threadPool = Executors.newFixedThreadPool(8, threadFactory(plugin, "ChessCraft-JDBI-%d"));
    this.queries = new QueriesLocator();
    this.jdbiExecutor = JdbiExecutor.create(this.jdbi, this.threadPool);
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onQuit(final PlayerQuitEvent e) {
    this.playerCache.synchronous().invalidate(e.getPlayer().getUniqueId());
  }

  private static ThreadFactory threadFactory(final ChessCraft plugin, final String nameFormat) {
    return new ThreadFactoryBuilder()
      .setNameFormat(nameFormat)
      .setUncaughtExceptionHandler((thr, ex) -> plugin.getSLF4JLogger().warn("Uncaught exception on thread {}", thr.getName(), ex))
      .build();
  }

  public void close() {
    Util.shutdownExecutor(this.threadPool, TimeUnit.SECONDS, 3);
    this.dataSource.close();
  }

  public CompletionStage<List<GameState>> queryIncompleteMatches(final UUID playerId) {
    return this.jdbiExecutor.withHandle(handle -> handle.createQuery(this.queries.query("select_incomplete_matches"))
      .bind("player_id", playerId)
      .mapTo(GameState.class)
      .list()
      .stream()
      .sorted(newestFirst())
      .toList());
  }

  public CompletionStage<List<GameState>> queryCompleteMatches(final UUID playerId) {
    return this.jdbiExecutor.withHandle(handle -> handle.createQuery(this.queries.query("select_complete_matches"))
      .bind("player_id", playerId)
      .mapTo(GameState.class)
      .list()
      .stream()
      .sorted(newestFirst())
      .toList());
  }

  public CompletionStage<Optional<GameState>> queryMatch(final UUID id) {
    return this.jdbiExecutor.withHandle(handle -> handle.createQuery(this.queries.query("select_match"))
      .bind("id", id)
      .mapTo(GameState.class)
      .findOne());
  }

  public CompletionStage<ChessPlayer> cachedPlayer(final UUID id) {
    final Player player = this.plugin.getServer().getPlayer(id);
    if (player != null) {
      return CompletableFuture.completedFuture(ChessPlayer.player(player));
    }
    return this.playerCache.get(id).thenApply(cached -> {
      if (cached != null) {
        return cached;
      }
      return ChessPlayer.offlinePlayer(Bukkit.getOfflinePlayer(id));
    });
  }

  private CompletionStage<Optional<ChessPlayer.CachedPlayer>> queryPlayer(final UUID id) {
    return this.jdbiExecutor.withHandle(handle -> this.queryPlayer(id, handle));
  }

  private Optional<ChessPlayer.CachedPlayer> queryPlayer(final UUID id, final Handle handle) {
    return handle.createQuery(this.queries.query("select_player"))
      .bind("id", id)
      .mapTo(ChessPlayer.CachedPlayer.class)
      .findOne();
  }

  public CompletionStage<List<Pair<UUID, Integer>>> queryLeaderboard(final int limit) {
    return this.jdbiExecutor.withHandle(handle -> handle.createQuery(this.queries.query("query_leaderboard"))
      .bind("limit", limit)
      .map((rs, ctx) -> Pair.of(
        ctx.findColumnMapperFor(UUID.class).orElseThrow().map(rs, "id", ctx),
        rs.getInt("rating")
      ))
      .collectIntoList());
  }

  public void saveMatchAsync(final GameState state, final boolean insertResult) {
    if (state.whiteCpu() && state.blackCpu()) {
      return;
    }
    this.threadPool.execute(() -> {
      try {
        this.saveMatch(state, insertResult);
      } catch (final Exception e) {
        this.plugin.getSLF4JLogger().warn("Failed to save match {}", state, e);
      }
    });
  }

  public void saveMatch(final GameState state, final boolean insertResult) {
    this.jdbi.useTransaction(handle -> {
      this.updatePlayers(state, handle, insertResult);

      handle.createUpdate(this.queries.query("insert_match"))
        .bind("id", state.id())
        .bind("white_cpu", state.whiteCpu())
        .bind("white_cpu_elo", state.whiteElo())
        .bind("white_player_id", state.whiteId())
        .bind("white_time_control", state.whiteTime())
        .bind("black_cpu", state.blackCpu())
        .bind("black_cpu_elo", state.blackElo())
        .bind("black_player_id", state.blackId())
        .bind("black_time_control", state.blackTime())
        .bind("moves", state.moves())
        .bind("current_fen", state.currentFen())
        .bind("cpu_move_delay", state.cpuMoveDelay())
        .bind("time_control_settings", state.timeControlSettings())
        .execute();

      if (insertResult) {
        Objects.requireNonNull(state.result(), "result");
        handle.createUpdate(this.queries.query("record_result"))
          .bind("id", state.id())
          .bind("result", state.result())
          .execute();
      }
    });
  }

  private void updatePlayers(final GameState state, final Handle handle, boolean insertResult) {
    if (!state.blackCpu() && !state.whiteCpu() && insertResult) {
      final Optional<ChessPlayer.CachedPlayer> white = this.queryPlayer(state.whiteId(), handle);
      final Optional<ChessPlayer.CachedPlayer> black = this.queryPlayer(state.blackId(), handle);
      final Elo.NewRatings newRatings = Elo.computeNewRatings(
        white.map(ChessPlayer.CachedPlayer::ratingData).orElseGet(Elo.RatingData::newPlayer),
        black.map(ChessPlayer.CachedPlayer::ratingData).orElseGet(Elo.RatingData::newPlayer),
        state.matchOutcome()
      );
      this.updatePlayer(handle, state.whiteId(), newRatings.playerOne());
      this.updatePlayer(handle, state.blackId(), newRatings.playerTwo());
    } else {
      if (!state.whiteCpu()) {
        this.updatePlayer(handle, state.whiteId(), null);
      }
      if (!state.blackCpu()) {
        this.updatePlayer(handle, state.blackId(), null);
      }
    }
  }

  private void updatePlayer(final Handle handle, final UUID playerId, Elo.@Nullable RatingData ratingData) {
    final Optional<ChessPlayer.CachedPlayer> existing = this.queryPlayer(playerId, handle);
    final Optional<Player> player = Optional.ofNullable(Bukkit.getPlayer(playerId));
    final String username = player.map(Player::getName)
      .orElseGet(() -> existing.map(c -> PlainTextComponentSerializer.plainText().serialize(c.name()))
        .orElseGet(() -> Objects.requireNonNull(Bukkit.getOfflinePlayer(playerId).getName(), "name")));
    final Component displayName = player.map(Player::displayName)
      .orElseGet(() -> existing.map(ChessPlayer.CachedPlayer::displayName)
        .orElseGet(() -> Component.text(username)));
    if (ratingData == null) {
      ratingData = existing.map(ChessPlayer.CachedPlayer::ratingData)
        .orElseGet(Elo.RatingData::newPlayer);
    }
    handle.createUpdate(this.queries.query("insert_player"))
      .bind("id", playerId)
      .bind("username", username)
      .bind("displayname", displayName)
      .bind("rating", ratingData.rating())
      .bind("peak_rating", ratingData.peakRating())
      .bind("rated_matches", ratingData.matches())
      .execute();
    this.playerCache.put(
      playerId,
      CompletableFuture.completedFuture(new ChessPlayer.CachedPlayer(
        playerId, Component.text(username), displayName, ratingData.rating(), ratingData.peakRating(), ratingData.matches()
      ))
    );
  }

  private static Comparator<GameState> newestFirst() {
    return Comparator.<GameState, Timestamp>comparing(state -> Objects.requireNonNull(state.lastUpdated(), "lastUpdated")).reversed();
  }

  public static Database init(final ChessCraft plugin) {
    plugin.getSLF4JLogger().info("Initializing database...");

    SQLDrivers.loadFrom(Database.class.getClassLoader());

    final DatabaseSettings settings = plugin.config().databaseSettings();

    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(settings.type == DatabaseSettings.DatabaseType.H2 ? h2Url(plugin) : settings.url);
    hikariConfig.setUsername(settings.type == DatabaseSettings.DatabaseType.H2 ? "" : settings.username);
    hikariConfig.setPassword(settings.type == DatabaseSettings.DatabaseType.H2 ? "" : settings.password);
    hikariConfig.setPoolName("ChessCraft-HikariPool");
    hikariConfig.setThreadFactory(threadFactory(plugin, "ChessCraft-Hikari-%d"));
    hikariConfig.setMaximumPoolSize(settings.connectionPool.maximumPoolSize);
    hikariConfig.setMinimumIdle(settings.connectionPool.minimumIdle);
    hikariConfig.setMaxLifetime(settings.connectionPool.maximumLifetime);
    hikariConfig.setKeepaliveTime(settings.connectionPool.keepaliveTime);
    hikariConfig.setConnectionTimeout(settings.connectionPool.connectionTimeout);

    final HikariDataSource dataSource = new HikariDataSource(hikariConfig);

    final Flyway flyway = Flyway.configure(Database.class.getClassLoader())
      .baselineVersion("0")
      .baselineOnMigrate(true)
      .locations("queries/migrations/mysql")
      .dataSource(dataSource)
      .validateMigrationNaming(true)
      .validateOnMigrate(true)
      .load();
    flyway.migrate();

    final Jdbi jdbi = Jdbi.create(dataSource)
      // TimeControl
      .registerColumnMapper(new TimeControlColumnMapper())
      .registerArgument(new TimeControlColumnMapper())
      // TimeControlSettings
      .registerColumnMapper(new TimeControlSettingsColumnMapper())
      .registerArgument(new TimeControlSettingsColumnMapper())
      // Component
      .registerColumnMapper(new ComponentColumnMapper())
      .registerArgument(new ComponentColumnMapper())
      // List<Move>
      .registerColumnMapper(new MoveListColumnMapper())
      .registerArgument(new MoveListColumnMapper())
      // GameState
      .registerRowMapper(new GameStateRowMapper())
      // UUID
      .registerColumnMapper(new NativeUUIDColumnMapper())
      // GameState.Result
      .registerColumnMapper(new ResultColumnMapper())
      .registerArgument(new ResultColumnMapper())
      // Fen
      .registerColumnMapper(new FenColumnMapper())
      .registerArgument(new FenColumnMapper())
      // CachedPlayer
      .registerRowMapper(new CachedPlayerRowMapper());

    plugin.getSLF4JLogger().info("Done.");

    return new Database(plugin, dataSource, jdbi);
  }

  private static String h2Url(ChessCraft plugin) {
    return "jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/database;MODE=MySQL";
  }
}
