package xyz.jpenilla.minecraftchess;

import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public interface ChessPlayer extends Audience {
  ChessPlayer CPU = new Cpu();

  Component displayName();

  record Player(UUID uuid) implements ChessPlayer, ForwardingAudience.Single {
    @Override
    public Audience audience() {
      return Objects.requireNonNull(Bukkit.getPlayer(this.uuid));
    }

    @Override
    public Component displayName() {
      return Objects.requireNonNull(Bukkit.getPlayer(this.uuid)).displayName();
    }
  }

  static ChessPlayer player(final org.bukkit.entity.Player player) {
    return new Player(player.getUniqueId());
  }

  final class Cpu implements ChessPlayer, ForwardingAudience.Single {
    private Cpu() {
    }

    @Override
    public Audience audience() {
      return Audience.empty();
    }

    @Override
    public Component displayName() {
      return Component.text("CPU");
    }
  }
}
