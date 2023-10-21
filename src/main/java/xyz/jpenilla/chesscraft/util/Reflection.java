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

import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.RegionAccessor;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Reflection {
  private static final @Nullable Method OLD_SPAWN;

  static {
    @Nullable Method method = null;
    try {
      method = RegionAccessor.class.getDeclaredMethod("spawn", Location.class, Class.class, org.bukkit.util.Consumer.class);
    } catch (final NoSuchMethodException ignore) {
    }
    OLD_SPAWN = method;
  }

  private Reflection() {
  }

  // On 9/21/23 Bukkit changed the method to take java Consumer instead of Bukkit's; the commodore transform doesn't apply to Paper plugins...
  @SuppressWarnings({"unchecked", "deprecation"})
  public static <T extends Entity> T spawn(final Location location, final Class<T> clazz, final Consumer<T> function) {
    if (OLD_SPAWN == null) {
      return location.getWorld().spawn(location, clazz, function);
    }
    try {
      return (T) OLD_SPAWN.invoke(location.getWorld(), location, clazz, (org.bukkit.util.Consumer<T>) function::accept);
    } catch (final ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
