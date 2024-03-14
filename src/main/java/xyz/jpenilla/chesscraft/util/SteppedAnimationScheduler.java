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
package xyz.jpenilla.chesscraft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public final class SteppedAnimationScheduler {
  private final Map<Integer, List<Runnable>> tasks = new HashMap<>();

  public @This SteppedAnimationScheduler step(final int delay, final Runnable task) {
    this.tasks.computeIfAbsent(delay, $ -> new ArrayList<>()).add(task);
    return this;
  }

  public CompletableFuture<Void> schedule(final JavaPlugin plugin, final long delay, final @Nullable CompletableFuture<Void> previous) {
    if (previous == null) {
      return this.schedule_(plugin, delay, null);
    }
    return previous.thenCompose($ -> this.schedule_(plugin, delay, previous));
  }

  private CompletableFuture<Void> schedule_(final JavaPlugin plugin, final long delay, final @Nullable CompletableFuture<Void> previous) {
    final Map<Integer, List<Runnable>> tasks = new HashMap<>(this.tasks);
    return new CompletableFuture<>() {
      private final BukkitTask task = new BukkitRunnable() {
        private long tick = 0;

        @Override
        public void run() {
          try {
            final List<Runnable> runThisTick = tasks.remove((int) this.tick);
            if (runThisTick != null) {
              for (final Runnable runnable : runThisTick) {
                runnable.run();
              }
            }
            if (tasks.isEmpty()) {
              complete(null);
              this.cancel();
            }
            this.tick++;
          } catch (final Exception e) {
            completeExceptionally(e);
            this.cancel();
          }
        }
      }.runTaskTimer(plugin, delay, 1);

      @Override
      public boolean cancel(final boolean mayInterruptIfRunning) {
        if (previous != null) {
          // pass cancellation upstream
          previous.cancel(mayInterruptIfRunning);
        }
        if (!this.task.isCancelled()) {
          this.task.cancel();
        }
        return super.cancel(mayInterruptIfRunning);
      }
    };
  }
}
