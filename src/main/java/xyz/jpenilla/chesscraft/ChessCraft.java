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
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.config.ConfigHelper;
import xyz.jpenilla.chesscraft.config.MainConfig;
import xyz.jpenilla.chesscraft.db.Database;
import xyz.jpenilla.chesscraft.util.StockfishProvider;

public final class ChessCraft extends JavaPlugin {
  private final Deque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();
  private BoardManager boardManager;
  private Database database;
  private @MonotonicNonNull MainConfig config;

  @Override
  public void onEnable() {
    this.reloadMainConfig();
    this.database = Database.init(this);
    final Path stockfishPath = new StockfishProvider(this, this.getDataFolder().toPath().resolve("engines"))
      .engine(this.config.stockfishEngine());
    this.boardManager = new BoardManager(this, stockfishPath);
    this.boardManager.load();
    new Commands(this).register();

    new Metrics(this, 17745);
  }

  @Override
  public void onDisable() {
    if (this.boardManager != null) {
      this.boardManager.close();
    }
    while (!this.shutdownTasks.isEmpty()) {
      this.shutdownTasks.poll().run();
    }
    if (this.database != null) {
      this.database.close();
    }
  }

  public BoardManager boardManager() {
    return this.boardManager;
  }

  public MainConfig config() {
    return this.config;
  }

  public Database database() {
    return this.database;
  }

  public void reloadMainConfig() {
    this.config = this.loadConfig();
    this.saveConfig(this.config);
  }

  private MainConfig loadConfig() {
    return ConfigHelper.loadConfig(MainConfig.class, this.configFile());
  }

  private void saveConfig(final MainConfig config) {
    ConfigHelper.saveConfig(this.configFile(), config);
  }

  private Path configFile() {
    return this.getDataFolder().toPath().resolve("config.yml");
  }

  public Deque<Runnable> shutdownTasks() {
    return this.shutdownTasks;
  }
}
