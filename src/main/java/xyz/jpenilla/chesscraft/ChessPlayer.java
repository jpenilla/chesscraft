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

import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public interface ChessPlayer extends Audience {
  ChessPlayer CPU = new Cpu();

  default Component displayName() {
    return this.name();
  }

  Component name();

  record Player(UUID uuid) implements ChessPlayer, ForwardingAudience.Single {
    @Override
    public Audience audience() {
      return player();
    }

    @Override
    public Component displayName() {
      return player().displayName();
    }

    private org.bukkit.entity.Player player() {
      return Objects.requireNonNull(Bukkit.getPlayer(this.uuid));
    }

    @Override
    public Component name() {
      return Component.text(this.player().getName());
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
    public Component name() {
      return Component.text("CPU");
    }
  }
}
