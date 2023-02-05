/*
 * minecraft-chess
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
package xyz.jpenilla.minecraftchess.config;

import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.function.Predicate;
import org.bukkit.NamespacedKey;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

public final class NamespacedKeySerializer extends ScalarSerializer<NamespacedKey> {
  public static final ScalarSerializer<NamespacedKey> INSTANCE = new NamespacedKeySerializer();

  private NamespacedKeySerializer() {
    super(TypeToken.get(NamespacedKey.class));
  }

  @Override
  public NamespacedKey deserialize(final Type type, final Object obj) throws SerializationException {
    final NamespacedKey key = NamespacedKey.fromString(obj.toString());
    if (key == null) {
      throw new SerializationException("Invalid dimension key " + obj);
    }
    return key;
  }

  @Override
  protected Object serialize(final NamespacedKey item, final Predicate<Class<?>> typeSupported) {
    return item.toString();
  }
}
