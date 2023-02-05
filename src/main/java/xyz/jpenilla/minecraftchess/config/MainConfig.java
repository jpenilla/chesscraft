package xyz.jpenilla.minecraftchess.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record MainConfig(String stockfishEngine) {
  public MainConfig() {
    this("15.1:AVX2");
  }
}
