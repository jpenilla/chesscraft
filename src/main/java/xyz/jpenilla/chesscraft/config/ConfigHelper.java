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

import io.leangen.geantyref.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.chesscraft.data.Rotation;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.display.BoardDisplaySettings;

public final class ConfigHelper {
  public static YamlConfigurationLoader createLoader(final Path file) {
    return YamlConfigurationLoader.builder()
      .nodeStyle(NodeStyle.BLOCK)
      .defaultOptions(options -> options.serializers(serializers -> {
        serializers.register(Vec3i.SERIALIZER);
        serializers.register(Vec3d.SERIALIZER);
        serializers.register(Rotation.SERIALIZER);
        serializers.registerExact(NamespacedKeySerializer.INSTANCE);
        serializers.register(new RGBAHexBukkitColorSerializer());
        serializers.registerExact(PieceOptions.class, PieceOptions.SERIALIZER);
        serializers.registerAll(ConfigurateComponentSerializer.configurate().serializers());
        serializers.registerExact(new TypeToken<>() {}, new BoardDisplaySettings.Serializer());
      }))
      .path(file)
      .build();
  }

  public static <T> T loadConfig(
    final TypeToken<T> configType,
    final Path path,
    final Supplier<T> defaultConfigFactory
  ) {
    try {
      if (Files.isRegularFile(path)) {
        final YamlConfigurationLoader loader = createLoader(path);
        final CommentedConfigurationNode node = loader.load();
        return Objects.requireNonNull(node.get(configType));
      } else {
        return defaultConfigFactory.get();
      }
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to load config of type '" + configType.getType().getTypeName() + "' from file at '" + path + "'.", ex);
    }
  }

  // For @ConfigSerializable types with no args constructor
  public static <T> T loadConfig(final Class<T> configType, final Path path) {
    return loadConfig(TypeToken.get(configType), path, () -> {
      try {
        return configType.getConstructor().newInstance();
      } catch (final ReflectiveOperationException ex) {
        throw new RuntimeException("Failed to create instance of type " + configType.getName() + ", does it have a public no args constructor?");
      }
    });
  }

  public static <T> void saveConfig(final Path path, final TypeToken<T> configType, final T config) {
    saveConfig(path, config, configType);
  }

  // For @ConfigSerializable types
  public static void saveConfig(final Path path, final Object config) {
    saveConfig(path, config, null);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void saveConfig(final Path path, final Object config, final @Nullable TypeToken<?> configType) {
    try {
      Files.createDirectories(path.getParent());
      final YamlConfigurationLoader loader = createLoader(path);
      final CommentedConfigurationNode node = loader.createNode();
      if (configType != null) {
        node.set((TypeToken) configType, config);
      } else {
        node.set(config);
      }
      loader.save(node);
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to save config of type '" + (configType != null ? configType.getType().getTypeName() : config.getClass().getName()) + "' to file at '" + path + "'.", ex);
    }
  }

}
