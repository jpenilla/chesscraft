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
package xyz.jpenilla.chesscraft.display;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.display.settings.AbstractDisplaySettings;
import xyz.jpenilla.chesscraft.util.Util;

@DefaultQualifier(NonNull.class)
public final class MessageLogDisplayAudience extends AbstractTextDisplayHolder implements Audience {
  private final List<Component> messages = new CopyOnWriteArrayList<>();
  private final int lines;
  private final AbstractDisplaySettings<?> settings;

  public MessageLogDisplayAudience(
    final JavaPlugin plugin,
    final Location location,
    final int lines,
    final AbstractDisplaySettings<?> settings
  ) {
    super(plugin, location);
    this.lines = lines;
    this.settings = settings;
  }

  @SuppressWarnings({"UnstableApiUsage", "deprecation"})
  @Override
  public void sendMessage(final Identity source, final Component message, final MessageType type) {
    this.messages.add(message);
    while (this.messages.size() > this.lines * 2) {
      this.messages.removeFirst();
    }
    this.updateIfEntityExists();
  }

  @Override
  protected void updateEntity(final TextDisplay display) {
    putMarker(display);
    this.settings.apply(display);

    display.text(Component.join(JoinConfiguration.newlines(), Util.peekLast(this.messages, this.lines)));
  }

  @Override
  public void stopUpdates() {
  }

  @Override
  public void updateNow() {
  }
}
