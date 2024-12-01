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

import java.lang.reflect.Type;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.display.settings.BoardStatusSettings;
import xyz.jpenilla.chesscraft.display.settings.MessageLogSettings;
import xyz.jpenilla.chesscraft.display.settings.PositionLabelSettings;

public interface BoardDisplaySettings<S> {
  DisplayType type();

  boolean removeAfterGame();

  S getOrCreateState(ChessCraft plugin, ChessBoard board);

  default void gameEnded(S state) {
  }

  void remove(S state);

  enum DisplayType {
    MESSAGE_LOG(MessageLogSettings.class),
    BOARD_STATUS(BoardStatusSettings.class),
    POSITION_LABELS(PositionLabelSettings.class);

    private final Class<? extends BoardDisplaySettings<?>> type;

    DisplayType(final Class<? extends BoardDisplaySettings<?>> type) {
      this.type = type;
    }
  }

  final class Serializer implements TypeSerializer<BoardDisplaySettings<?>> {
    private static final String TYPE_FIELD_NAME = "type";

    public Serializer() {
    }

    @Override
    public BoardDisplaySettings<?> deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
      final DisplayType mode = node.node(TYPE_FIELD_NAME).get(DisplayType.class);
      if (mode == null) {
        throw new SerializationException("Missing mode, should be one of: " + Arrays.toString(DisplayType.values()));
      }
      return node.get(mode.type);
    }

    @Override
    public void serialize(final Type type, final @Nullable BoardDisplaySettings<?> obj, final ConfigurationNode node) throws SerializationException {
      if (obj == null) {
        throw new SerializationException("null");
      }
      final DisplayType mode = obj.type();
      node.node(TYPE_FIELD_NAME).set(mode);
      node.set(mode.type, obj);
    }
  }
}
