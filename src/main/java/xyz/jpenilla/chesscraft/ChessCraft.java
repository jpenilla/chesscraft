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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.config.ConfigHelper;
import xyz.jpenilla.chesscraft.config.MainConfig;
import xyz.jpenilla.chesscraft.util.StockfishProvider;

public final class ChessCraft extends JavaPlugin {
  private BoardManager boardManager;
  private @MonotonicNonNull MainConfig config;

  @Override
  public void onEnable() {
    this.reloadMainConfig();
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
  }

  public BoardManager boardManager() {
    return this.boardManager;
  }

  public MainConfig config() {
    return this.config;
  }

  public void reloadMainConfig() {
    this.config = this.loadConfig();
    this.saveConfig(this.config);
  }

  private MainConfig loadConfig() {
    final Path file = this.getDataFolder().toPath().resolve("config.yml");
    try {
      Files.createDirectories(file.getParent());
      if (Files.isRegularFile(file)) {
        final YamlConfigurationLoader loader = ConfigHelper.createLoader(file);
        final CommentedConfigurationNode node = loader.load();
        return Objects.requireNonNull(node.get(MainConfig.class));
      } else {
        return new MainConfig();
      }
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void saveConfig(final MainConfig config) {
    final Path file = this.getDataFolder().toPath().resolve("config.yml");
    try {
      Files.createDirectories(file.getParent());
      final YamlConfigurationLoader loader = ConfigHelper.createLoader(file);
      final CommentedConfigurationNode node = loader.createNode();
      node.set(config);
      loader.save(node);
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
