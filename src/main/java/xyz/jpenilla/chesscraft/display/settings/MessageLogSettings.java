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
package xyz.jpenilla.chesscraft.display.settings;

import org.bukkit.entity.TextDisplay;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.display.MessageLogDisplayAudience;

@ConfigSerializable
public final class MessageLogSettings extends AbstractDisplaySettings.WithTransformation<MessageLogDisplayAudience> {
  public MessageLogSettings() {
    this.alignment = TextDisplay.TextAlignment.LEFT;
    this.offset = new Vec3d(4, 3, -4);
  }

  private int lines = 2;

  @Override
  public DisplayType type() {
    return DisplayType.MESSAGE_LOG;
  }

  @Override
  public MessageLogDisplayAudience getOrCreateState(final ChessCraft plugin, final ChessBoard board) {
    return new MessageLogDisplayAudience(
      plugin,
      this.offsetNudged(board).toLocation(board.world()),
      this.lines,
      this
    );
  }

  @Override
  public void gameEnded(final MessageLogDisplayAudience state) {
    if (this.removeAfterGame()) {
      state.remove();
    }
  }
}
