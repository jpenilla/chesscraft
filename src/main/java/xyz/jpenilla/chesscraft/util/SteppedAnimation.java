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
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

public final class SteppedAnimation {
  private final int startDelay;
  private final HashMap<Integer, List<Runnable>> steps;

  private SteppedAnimation(final int startDelay, final Map<Integer, List<Runnable>> steps) {
    this.startDelay = startDelay;
    this.steps = new HashMap<>(steps);
  }

  public boolean tick(final int tick) {
    final @Nullable List<Runnable> remove = this.steps.remove(tick);
    if (remove != null) {
      for (final Runnable runnable : remove) {
        runnable.run();
      }
    }
    return !this.steps.isEmpty();
  }

  public static final class Scheduler {
    private final Deque<Supplier<SteppedAnimation>> animations = new ConcurrentLinkedDeque<>();
    private final BukkitTask task;
    private int tick = 0;
    private volatile @Nullable SteppedAnimation current;

    public Scheduler(final JavaPlugin plugin) {
      this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
    }

    private void tick() {
      if (this.current == null) {
        final @Nullable Supplier<SteppedAnimation> anim = this.animations.pollFirst();
        if (anim != null) {
          this.current = anim.get();
          this.tick = 0;
        }
      }

      final @Nullable SteppedAnimation current = this.current;
      if (current != null) {
        if (this.tick < current.startDelay || current.tick(this.tick - current.startDelay)) {
          this.tick++;
        } else {
          this.current = null;
        }
      }
    }

    public void schedule(final Supplier<SteppedAnimation> animationFactory) {
      this.animations.addLast(animationFactory);
    }

    public void clearCurrent() {
      this.animations.clear();
      this.current = null;
    }

    public void cancel() {
      this.task.cancel();
      this.clearCurrent();
    }
  }

  public static final class Builder {
    private final Map<Integer, List<Runnable>> steps = new HashMap<>();
    private int startDelay = 0;

    public DelayScope step(final int delay, final Runnable task) {
      this.steps.computeIfAbsent(delay, $ -> new ArrayList<>()).add(task);
      return new DelayScope(delay);
    }

    public SteppedAnimation.@This Builder startDelay(final int startDelay) {
      this.startDelay = startDelay;
      return this;
    }

    public final class DelayScope {
      private final int delay;

      private DelayScope(final int delay) {
        this.delay = delay;
      }

      public DelayScope then(final int delay, final Runnable task) {
        return Builder.this.step(this.delay + delay, task);
      }

      public Builder exitScope() {
        return Builder.this;
      }

      public SteppedAnimation build() {
        return Builder.this.build();
      }
    }

    public SteppedAnimation build() {
      return new SteppedAnimation(this.startDelay, this.steps);
    }
  }
}
