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
            if (!file.toFile().setExecutable(true, true)) {
              this.plugin.getLogger().warning("Failed to set extracted file " + file + " executable, this may cause issues");
            }
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
