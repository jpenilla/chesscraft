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
package xyz.jpenilla.chesscraft.data;

import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

@DefaultQualifier(NonNull.class)
public record Rotation(double x, double y, double z, double w) {
  public static final ScalarSerializer<Rotation> SERIALIZER = new Serializer();
  public static final Rotation DEFAULT = new Rotation(0, 0, 0, 1);

  private static final class Serializer extends ScalarSerializer<Rotation> {
    private Serializer() {
      super(TypeToken.get(Rotation.class));
    }

    @Override
    public Rotation deserialize(final Type type, final Object obj) throws SerializationException {
      final String[] split = obj.toString().split(" ");
      final double x = tryParse(split[0]);
      final double y = tryParse(split[1]);
      final double z = tryParse(split[2]);
      final double w = tryParse(split[3]);
      return new Rotation(x, y, z, w);
    }

    private static double tryParse(final String s) throws SerializationException {
      try {
        return Double.parseDouble(s);
      } catch (final NumberFormatException ex) {
        throw new SerializationException(ex);
      }
    }

    @Override
    protected Object serialize(final Rotation item, final Predicate<Class<?>> typeSupported) {
      return item.x() + " " + item.y() + " " + item.z() + " " + item.w();
    }
  }
}
