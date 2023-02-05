package xyz.jpenilla.minecraftchess.config;

import java.nio.file.Path;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.minecraftchess.data.Vec3;

public final class ConfigHelper {
  public static YamlConfigurationLoader createLoader(final Path file) {
    return YamlConfigurationLoader.builder()
      .nodeStyle(NodeStyle.BLOCK)
      .defaultOptions(options -> options.serializers(serializers -> {
        serializers.register(Vec3.SERIALIZER);
        serializers.register(NamespacedKeySerializer.INSTANCE);
      }))
      .path(file)
      .build();
  }
}
