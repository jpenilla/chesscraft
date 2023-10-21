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
package xyz.jpenilla.chesscraft.config;

import java.lang.reflect.Type;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.serialize.ScalarSerializer;

@DefaultQualifier(NonNull.class)
final class RGBAHexBukkitColorSerializer extends ScalarSerializer<Color> {
  public RGBAHexBukkitColorSerializer() {
    super(Color.class);
  }

  @Override
  public Color deserialize(final Type type, final Object obj) {
    return Color.fromARGB(parseHex(obj.toString()));
  }

  @Override
  protected Object serialize(final Color item, final Predicate<Class<?>> typeSupported) {
    return String.format("#%08X", argbToRgba(item.asARGB()));
  }

  public static int parseHex(final String color) {
    final int rgba = (int) Long.parseLong(color.replace("#", ""), 16);
    if (color.length() == 9) {
      return rgbaToArgb(rgba);
    }
    return rgba;
  }

  public static int argbToRgba(final int color) {
    final int a = color >> 24 & 0xFF;
    final int r = color >> 16 & 0xFF;
    final int g = color >> 8 & 0xFF;
    final int b = color & 0xFF;
    return r << 24 | g << 16 | b << 8 | a;
  }

  public static int rgbaToArgb(final int color) {
    final int r = color >> 24 & 0xFF;
    final int g = color >> 16 & 0xFF;
    final int b = color >> 8 & 0xFF;
    final int a = color & 0xFF;
    return a << 24 | r << 16 | g << 8 | b;
  }
}
