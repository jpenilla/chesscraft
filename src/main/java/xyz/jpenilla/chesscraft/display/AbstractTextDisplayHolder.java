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
package xyz.jpenilla.chesscraft.display;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.util.Reflection;
import xyz.jpenilla.chesscraft.util.Util;

@DefaultQualifier(NonNull.class)
public abstract class AbstractTextDisplayHolder {
  private static final NamespacedKey MARKER = new NamespacedKey("chesscraft", "text_display");

  private final JavaPlugin plugin;
  private final Location pos;
  private volatile @Nullable UUID entityId;

  public AbstractTextDisplayHolder(
    final JavaPlugin plugin,
    final Location pos
  ) {
    this.plugin = plugin;
    this.pos = pos;
  }

  public abstract void stopUpdates();

  public abstract void updateNow();

  protected abstract void updateEntity(TextDisplay display);

  public JavaPlugin plugin() {
    return this.plugin;
  }

  public void remove() {
    this.stopUpdates();
    Util.scheduleOrRun(this.plugin, () -> {
      final @Nullable TextDisplay entity = this.entity();
      if (entity != null) {
        entity.remove();
      }
      this.entityId = null;
    });
  }

  public void ensureSpawned() {
    if (this.entity() == null) {
      final Collection<Entity> nearby = this.pos.getNearbyEntities(0.1, 0.1, 0.1);
      final @Nullable TextDisplay existing = nearby.stream().filter(TextDisplay.class::isInstance).map(TextDisplay.class::cast)
        .filter(e -> e.getPersistentDataContainer().has(MARKER)).findFirst().orElse(null);
      if (existing != null) {
        existing.teleport(this.pos);
        this.entityId = existing.getUniqueId();
        this.updateEntity(existing);
      } else {
        final TextDisplay spawned = Reflection.spawn(this.pos, TextDisplay.class, this::updateEntity);
        this.entityId = spawned.getUniqueId();
      }
    }
  }

  protected @Nullable TextDisplay entity() {
    final @Nullable UUID id = this.entityId;
    if (id == null) {
      return null;
    }
    return (TextDisplay) this.pos.getWorld().getEntity(id);
  }

  protected void updateIfEntityExists() {
    if (this.entityId != null) {
      Util.scheduleOrRun(this.plugin, () -> {
        final @Nullable TextDisplay entity = this.entity();
        if (entity != null) {
          this.updateEntity(entity);
        }
      });
    }
  }

  protected static void putMarker(final TextDisplay display) {
    display.getPersistentDataContainer().set(MARKER, PersistentDataType.BYTE, (byte) 1);
  }
}
