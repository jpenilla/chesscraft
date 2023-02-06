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
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.config.ConfigHelper;
import xyz.jpenilla.chesscraft.config.MainConfig;

public final class ChessCraft extends JavaPlugin {
  private BoardManager boardManager;

  @Override
  public void onEnable() {
    final MainConfig config = this.loadConfig();
    this.saveConfig(config);
    final Path stockfishPath = new StockfishProvider(this, this.getDataFolder().toPath().resolve("engines"))
      .engine(config.stockfishEngine());
    this.boardManager = new BoardManager(this, stockfishPath);
    this.boardManager.load();
    new Commands(this).register();
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

  @Override
  public void onDisable() {
    if (this.boardManager != null) {
      this.boardManager.close();
    }
  }

  public BoardManager boardManager() {
    return this.boardManager;
  }
}
