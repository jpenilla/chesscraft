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
import org.bukkit.OfflinePlayer;
import xyz.jpenilla.chesscraft.util.Elo;

public interface ChessPlayer extends Audience {
  default Component displayName() {
    return this.name();
  }

  Component name();

  default boolean isCpu() {
    return this instanceof Cpu;
  }

  static Player player(final org.bukkit.entity.Player player) {
    return new OnlinePlayer(player.getUniqueId());
  }

  interface Player extends ChessPlayer {
    UUID uuid();
  }

  static Player offlinePlayer(final OfflinePlayer offlinePlayer) {
    return new Player() {
      @Override
      public Component name() {
        return Component.text(offlinePlayer.getName());
      }

      @Override
      public Component displayName() {
        if (offlinePlayer.isOnline()) {
          return offlinePlayer.getPlayer().displayName();
        }
        return Player.super.displayName();
      }

      @Override
      public UUID uuid() {
        return offlinePlayer.getUniqueId();
      }
    };
  }

  record CachedPlayer(
    UUID uuid,
    Component name,
    Component displayName,
    int rating,
    int peakRating,
    int ratedMatches
  ) implements Player {
    public Elo.RatingData ratingData() {
      return new Elo.RatingData(this.rating, this.peakRating, this.ratedMatches);
    }
  }

  static ChessPlayer cpu(final int elo) {
    return new Cpu(elo, UUID.randomUUID());
  }

  record OnlinePlayer(UUID uuid) implements Player, ForwardingAudience.Single {
    @Override
    public Audience audience() {
      return player();
    }

    @Override
    public Component displayName() {
      return player().displayName();
    }

    @Override
    public Component name() {
      return Component.text(this.player().getName());
    }

    public org.bukkit.entity.Player player() {
      return Objects.requireNonNull(Bukkit.getPlayer(this.uuid), "Player with UUID " + this.uuid + " not logged in.");
    }
  }

  // UUID so two CPUs with same elo aren't equal
  record Cpu(int elo, UUID id) implements ChessPlayer, ForwardingAudience.Single {
    private static final Component NAME = Component.text("CPU");

    @Override
    public Audience audience() {
      return Audience.empty();
    }

    @Override
    public Component name() {
      return NAME;
    }
  }
}
