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

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.niflheim.stockfish.engine.enums.Variant;

public final class StockfishProvider {
  private static final String STOCKFISH_REPO = "official-stockfish/Stockfish";
  private static final String GITHUB_RELEASE_LIST = "https://api.github.com/repos/" + STOCKFISH_REPO + "/releases";
  private static final String ARCHIVE_BASE_URL = "https://files.stockfishchess.org/archive/";
  private static final String LEGACY_BASE_URL = "https://files.stockfishchess.org/files/";
  private static final String LEGACY_BASE_URL_INTERNET_ARCHIVE = "https://web.archive.org/web/20230311015912id_/https://stockfishchess.org/files/";

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
      final Path customPath = configValue.startsWith("/")
        ? Path.of(configValue)
        : this.dir.resolve("custom").resolve(configValue);
      if (!customPath.toFile().canExecute()) {
        if (!customPath.toFile().setExecutable(true, true)) {
          this.plugin.getLogger().warning("Custom engine '{}' was not executable and ChessCraft failed to set it to executable, this may cause issues.");
        }
      }
      return customPath;
    }
    final String[] split = configValue.split(":");
    final String version = split[0];

    final String[] ver = version.split("\\.");
    final int major = Integer.parseInt(ver[0]);

    final Variant variant;
    if (split[1].equalsIgnoreCase("auto")) {
      variant = ProcessorUtil.bestVariant(this.plugin.getSLF4JLogger(), major < 16);
    } else {
      variant = Variant.valueOf(split[1].toUpperCase(Locale.ENGLISH));
    }
    String path = "managed/" + variant.fileName(windows(), version, false);
    if (major > 15 && macOS()) {
      path = path.replace("linux", "mac").replace("popcnt", "modern");
    }
    final Path file = this.dir.resolve(path);
    if (Files.isRegularFile(file)) {
      return file;
    }
    final List<String> urls = urls(version, variant, major > 15);
    try {
      for (final String url : urls) {
        if (this.tryDownload(version, variant, file, url)) {
          break;
        }
      }
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
    this.plugin.getLogger().info("Done!");
    return file;
  }

  private static List<String> urls(final String version, final Variant variant, final boolean sf16Plus) {
    final String downloadZipName = variant.fileName(windows(), version, true);
    if (!sf16Plus) {
      return List.of(
        ARCHIVE_BASE_URL + "Stockfish " + version + "/" + downloadZipName,
        LEGACY_BASE_URL + downloadZipName,
        LEGACY_BASE_URL_INTERNET_ARCHIVE + downloadZipName
      );
    }

    final JsonArray result;
    try {
      result = new Gson().fromJson(new InputStreamReader(URI.create(GITHUB_RELEASE_LIST).toURL().openStream(), Charsets.UTF_8), JsonArray.class);
    } catch (final IOException exception) {
      throw new UncheckedIOException("Failed to fetch Stockfish release list from GitHub.", exception);
    }

    @Nullable JsonObject found = null;
    for (final JsonElement element : result) {
      if (!(element instanceof JsonObject object)) {
        continue;
      }
      final String tagName = object.get("tag_name").getAsString();
      // trim 'sf_'
      if (tagName.length() > 3 && tagName.substring(3).equals(version)) {
        found = object;
        break;
      }
    }

    if (found == null) {
      throw new IllegalArgumentException("Could not find Stockfish release sf_" + version + " on GitHub.");
    }

    @Nullable JsonObject foundAsset = null;
    for (final JsonElement assetElement : found.get("assets").getAsJsonArray()) {
      if (!(assetElement instanceof JsonObject assetObject)) {
        continue;
      }
      final String assetName = assetObject.get("name").getAsString();
      if (!checkVariant(variant, assetName) || !checkOs(assetName)) {
        continue;
      }
      foundAsset = assetObject;
      break;
    }

    if (foundAsset == null) {
      throw new IllegalStateException("Could not find download Stockfish " + version + " " + variant + " for " + System.getProperty("os.name"));
    }

    final String url = foundAsset.get("browser_download_url").getAsString();

    return List.of(url);
  }

  private static boolean checkVariant(final Variant variant, final String name) {
    return switch (variant) {
      case DEFAULT -> !name.contains("avx2") && !name.contains("bmi2") && !name.contains("modern");
      case AVX2 -> name.contains("avx2");
      case BMI2 -> name.contains("bmi2");
      case POPCNT -> name.contains("modern");
    };
  }

  private static boolean checkOs(final String name) {
    if (windows()) {
      return name.contains("win");
    } else if (macOS()) {
      return name.contains("mac");
    }
    return name.contains("ubuntu") || name.contains("linux");
  }

  private boolean tryDownload(final String version, final Variant variant, final Path file, final String urlString) throws IOException {
    final URL url = URI.create(urlString).toURL();
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.connect();
    if (conn.getResponseCode() != 200) {
      return false;
    }

    final String variantString = macOS() && variant == Variant.POPCNT ? "MODERN" : variant.toString();
    this.plugin.getLogger().info("Downloading Stockfish " + version + " (" + variantString + ") from '" + url + "'...");

    final Path temp = Files.createTempFile("stockfish-" + version + "-" + variant + "-download", null);
    try (final InputStream stream = url.openStream();
      final FileOutputStream out = new FileOutputStream(temp.toFile())) {
      final ReadableByteChannel channel = Channels.newChannel(stream);
      out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
    }
    final boolean found;
    if (urlString.endsWith(".zip")) {
      found = this.extractFromZip(temp, file);
    } else {
      found = this.extractFromTar(temp, file, urlString);
    }
    if (!found) {
      throw new IllegalStateException("Could not find stockfish executable in downloaded archive");
    }
    if (!file.toFile().setExecutable(true, true)) {
      this.plugin.getLogger().warning("Failed to set extracted file " + file + " executable, this may cause issues");
    }
    Files.delete(temp);
    return true;
  }

  private boolean extractFromZip(final Path archive, final Path dest) throws IOException {
    try (final FileSystem fs = FileSystems.newFileSystem(archive, new HashMap<>());
      final Stream<Path> s = Files.walk(fs.getPath("/"))) {
      for (final Path path : s.toList()) {
        if (Files.isRegularFile(path)
          && (path.getFileName().toString().startsWith("stockfish-") || path.getFileName().toString().startsWith("stockfish_"))) {
          Files.createDirectories(dest.getParent());
          Files.copy(path, dest);
          return true;
        }
      }
    }
    return false;
  }

  private boolean extractFromTar(final Path archive, final Path dest, final String urlString) throws IOException {
    final boolean gzip = urlString.endsWith(".gz");
    try (
      final BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(archive));
      final TarArchiveInputStream tar = new TarArchiveInputStream(
        gzip ? new GzipCompressorInputStream(inputStream) : inputStream
      )
    ) {
      TarArchiveEntry entry;
      while ((entry = tar.getNextEntry()) != null) {
        final String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
        if (!entry.isDirectory()
          && (fileName.startsWith("stockfish-") || fileName.startsWith("stockfish_"))) {
          Files.createDirectories(dest.getParent());
          Files.copy(tar, dest);
          return true;
        }
      }
    }
    return false;
  }

  private static boolean windows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  private static boolean macOS() {
    return System.getProperty("os.name").toLowerCase().contains("mac");
  }
}
