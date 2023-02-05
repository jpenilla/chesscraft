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
