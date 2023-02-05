package xyz.jpenilla.minecraftchess;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import xyz.niflheim.stockfish.engine.enums.Variant;

public final class StockfishProvider {
  private static final String BASE_URL = "https://stockfishchess.org/files/";

  private final Path dir;
  private final MinecraftChess plugin;

  public StockfishProvider(final MinecraftChess plugin, final Path enginesDir) {
    this.plugin = plugin;
    this.dir = enginesDir;
    for (final Path path : List.of(this.dir.resolve("custom"), this.dir.resolve("managed"))) {
      try {
        Files.createDirectories(path);
      } catch (final IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public Path engine(final String configValue) {
    if (!configValue.contains(":")) {
      return this.dir.resolve("custom/" + configValue);
    }
    final String[] split = configValue.split(":");
    final String version = split[0];
    final Variant variant = Variant.valueOf(split[1].toUpperCase(Locale.ENGLISH));
    final Path file = this.dir.resolve("managed/" + variant.fileName(windows(), version, false));
    if (Files.isRegularFile(file)) {
      return file;
    }
    final String downloadZipName = variant.fileName(windows(), version, true);
    final String url = BASE_URL + downloadZipName;
    this.plugin.getLogger().info("Downloading Stockfish " + version + " (" + variant + ") from " + url + "...");
    try {
      final Path temp = Files.createTempFile("stockfish-" + version + "-" + variant + "-download", null);
      try (final InputStream stream = new URL(url).openStream();
        final FileOutputStream out = new FileOutputStream(temp.toFile())) {
        final ReadableByteChannel channel = Channels.newChannel(stream);
        out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
      }
      try (final FileSystem fs = FileSystems.newFileSystem(temp, new HashMap<>());
        final Stream<Path> s = Files.walk(fs.getPath("/"))) {
        for (final Path path : s.toList()) {
          if (Files.isRegularFile(path) && path.getFileName().toString().startsWith("stockfish-")) {
            Files.createDirectories(file.getParent());
            Files.copy(path, file);
            break;
          }
        }
      }
      Files.delete(temp);
      this.plugin.getLogger().info("Done!");
      return file;
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean windows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
