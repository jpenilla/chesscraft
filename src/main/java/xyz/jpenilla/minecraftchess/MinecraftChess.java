package xyz.jpenilla.minecraftchess;

import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftChess extends JavaPlugin {
  private BoardManager boardManager;

  @Override
  public void onEnable() {
    this.boardManager = new BoardManager(this);
    this.boardManager.load();
    new Commands(this).register();
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
