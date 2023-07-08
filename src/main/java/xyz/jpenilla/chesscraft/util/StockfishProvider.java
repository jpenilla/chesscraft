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
package xyz.jpenilla.chesscraft.util;

import java.io.FileNotFoundException;
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
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.niflheim.stockfish.engine.enums.Variant;

public final class StockfishProvider {
  // Stockfish removed their pre-v16 downloads, new downloads for v16+ are on GitHub Releases.
  // archive.org has some of the removed versions, but not all.
  // The timestamp in the URL is for the newest archived build of 15.1, but archive.org seems to redirect
  // each URL to the most recently archived version of that URL if there isn't a snapshot for the
  // exact timestamp provided.
  // Hopefully no one decides to archive the new redirect to the downloads page, because then we would
  // likely need to use archive.orgs API to determine which snapshot to use.

  // Stockfish offers their own (mostly complete) archive, but it's hosted on Dropbox which makes
  // it more difficult to use.
  // TODO: retrieve versions 16+ through github release artifacts
  // TODO: fix cpu feature detection & downloads on macos arm64 (rosetta) (for versions macos builds exist)
  private static final String BASE_URL = "https://web.archive.org/web/20230311015912id_/https://stockfishchess.org/files/";

  private final Path dir;
  private final ChessCraft plugin;

  public StockfishProvider(final ChessCraft plugin, final Path enginesDir) {
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

    final String[] ver = version.split("\\.");
    final int major = Integer.parseInt(ver[0]);
    if (major > 15) {
      throw new IllegalArgumentException("Support for automatically downloading Stockfish 16+ is not yet available." +
        "\nEither manually download it from https://stockfishchess.org/download/, or use an older version such as 15.1.");
    }

    Variant variant;
    if (split[1].equalsIgnoreCase("auto")) {
      variant = ProcessorUtil.bestVariant(this.plugin.getSLF4JLogger());
    } else {
      variant = Variant.valueOf(split[1].toUpperCase(Locale.ENGLISH));
    }
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
        boolean found = false;
        for (final Path path : s.toList()) {
          if (Files.isRegularFile(path)
            && (path.getFileName().toString().startsWith("stockfish-") || path.getFileName().toString().startsWith("stockfish_"))) {
            Files.createDirectories(file.getParent());
            Files.copy(path, file);
            if (!file.toFile().setExecutable(true, true)) {
              this.plugin.getLogger().warning("Failed to set extracted file " + file + " executable, this may cause issues");
            }
            found = true;
            break;
          }
        }
        if (!found) {
          throw new IllegalStateException("Could not find stockfish executable in downloaded archive");
        }
      }
      Files.delete(temp);
      this.plugin.getLogger().info("Done!");
      return file;
    } catch (final FileNotFoundException ex) {
      if (url.contains("archive.org")) {
        throw new RuntimeException(
          String.format("""
              archive.org did not have the requested Stockfish build (version=%s, variant=%s).
              See 'https://web.archive.org/web/*/https://stockfishchess.org/files/*' for the list of archived builds.
              archive.org is used to resolve builds for Stockfish 15.1 and older as Stockfish has removed these downloads from their site.""",
            version, variant),
          ex
        );
      }
      throw new RuntimeException(ex);
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean windows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
