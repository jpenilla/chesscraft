package xyz.jpenilla.minecraftchess.data;

import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

public record Vec3(int x, int y, int z) {
  public static ScalarSerializer<Vec3> SERIALIZER = new Serializer();

  public static Vec3 fromLocation(final Location loc) {
    return new Vec3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  private static final class Serializer extends ScalarSerializer<Vec3> {
    private Serializer() {
      super(TypeToken.get(Vec3.class));
    }

    @Override
    public Vec3 deserialize(final Type type, final Object obj) throws SerializationException {
      final String[] split = obj.toString().split(" ");
      final int x = tryParseInt(split[0]);
      final int y = tryParseInt(split[1]);
      final int z = tryParseInt(split[2]);
      return new Vec3(x, y, z);
    }

    private static int tryParseInt(final String s) throws SerializationException {
      try {
        return Integer.parseInt(s);
      } catch (final NumberFormatException ex) {
        throw new SerializationException(ex);
      }
    }

    @Override
    protected Object serialize(final Vec3 item, final Predicate<Class<?>> typeSupported) {
      return item.x() + " " + item.y() + " " + item.z();
    }
  }
}
