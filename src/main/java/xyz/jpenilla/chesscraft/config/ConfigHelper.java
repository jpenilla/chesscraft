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

import java.nio.file.Path;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.chesscraft.data.Vec3;

public final class ConfigHelper {
  public static YamlConfigurationLoader createLoader(final Path file) {
    return YamlConfigurationLoader.builder()
      .nodeStyle(NodeStyle.BLOCK)
      .defaultOptions(options -> options.serializers(serializers -> {
        serializers.register(Vec3.SERIALIZER);
        serializers.register(NamespacedKeySerializer.INSTANCE);
        serializers.registerExact(PieceOptions.class, PieceOptions.SERIALIZER);
      }))
      .path(file)
      .build();
  }
}
