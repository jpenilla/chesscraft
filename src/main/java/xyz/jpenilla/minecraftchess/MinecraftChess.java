package xyz.jpenilla.minecraftchess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.minecraftchess.command.Commands;
import xyz.jpenilla.minecraftchess.config.ConfigHelper;
import xyz.jpenilla.minecraftchess.config.MainConfig;

public final class MinecraftChess extends JavaPlugin {
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
