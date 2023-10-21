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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.display.settings.AbstractDisplaySettings;

@DefaultQualifier(NonNull.class)
public final class BoardBoundUpdatingText extends AbstractTextDisplayHolder {
  private final ChessBoard board;
  private final @Nullable BukkitTask update;
  private final @Nullable Runnable updateRunnable;
  private final AbstractDisplaySettings<?> settings;
  private final AtomicReference<Component> text = new AtomicReference<>();

  public BoardBoundUpdatingText(
    final JavaPlugin plugin,
    final ChessBoard board,
    final Location pos,
    final Supplier<List<Component>> textSupplier,
    final long updateRate,
    final AbstractDisplaySettings<?> settings
  ) {
    super(plugin, pos);
    this.board = board;
    this.updateRunnable = () -> this.text(textSupplier.get());
    this.update = updateRate > 0 ? plugin.getServer().getScheduler().runTaskTimer(plugin, this.updateRunnable, 0L, updateRate) : null;
    this.settings = settings;
  }

  public ChessBoard board() {
    return this.board;
  }

  public void text(final List<Component> lines) {
    final Component n = Component.join(
      JoinConfiguration.newlines(),
      lines
    );
    final @Nullable Component old = this.text.getAndSet(n);
    if (!n.equals(old)) {
      this.updateIfEntityExists();
    }
  }

  @Override
  protected void updateEntity(final TextDisplay display) {
    putMarker(display);
    this.settings.apply(display);

    display.text(this.text.get());
  }

  @Override
  public void updateNow() {
    if (this.updateRunnable != null) {
      this.updateRunnable.run();
    }
  }

  @Override
  public void stopUpdates() {
    if (this.update != null) {
      this.update.cancel();
    }
  }
}
