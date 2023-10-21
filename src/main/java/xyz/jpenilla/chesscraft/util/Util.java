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
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessCraft;

@DefaultQualifier(NonNull.class)
public final class Util {
  private Util() {
  }

  public static void scheduleOrRun(final JavaPlugin plugin, final Runnable runnable) {
    if (plugin.getServer().isStopping()) {
      ((ChessCraft) plugin).shutdownTasks().add(runnable);
    } else if (plugin.getServer().isPrimaryThread()) {
      runnable.run();
    } else {
      plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
  }

  public static void schedule(final JavaPlugin plugin, final Runnable runnable) {
    if (plugin.getServer().isStopping()) {
      ((ChessCraft) plugin).shutdownTasks().add(runnable);
    } else {
      plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
  }

  /**
   * Peek at the last {@code x} or fewer elements.
   *
   * <p>Does not support concurrent collections.</p>
   *
   * @param list list
   * @param x    amount
   * @return peek
   */
  public static List<Component> peekLast(final List<Component> list, final int x) {
    final List<Component> trimmed = new ArrayList<>(Math.max(list.size(), x));
    for (int i = list.size() - 1; i >= 0 && trimmed.size() < x; i--) {
      trimmed.add(list.get(i));
    }
    Collections.reverse(trimmed);
    return trimmed;
  }
}
