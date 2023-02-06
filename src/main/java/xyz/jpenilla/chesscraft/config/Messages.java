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
package xyz.jpenilla.chesscraft.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

@ConfigSerializable
public final class Messages {
  private String checkmateMessage = "<winner_color><winner_name></winner_color> <green>beat <loser_color><loser_name></loser_color> by checkmate!";

  public Component checkmateMessage(final ChessPlayer black, final ChessPlayer white, final PieceColor winner) {
    final ChessPlayer win = winner == PieceColor.BLACK ? black : white;
    final ChessPlayer loss = winner == PieceColor.BLACK ? white : black;
    return MiniMessage.miniMessage().deserialize(
      this.checkmateMessage,
      TagResolver.resolver("winner_color", Tag.styling(winner.textColor())),
      TagResolver.resolver("loser_color", Tag.styling(winner.other().textColor())),
      Placeholder.component("winner_name", win.name()),
      Placeholder.component("loser_name", loss.name())
    );
  }

  private String stalemateMessage = "<black><black_name></black> <green>ended in a stalemate with <white><white_name></white>!";

  public Component stalemateMessage(final ChessPlayer black, final ChessPlayer white) {
    return MiniMessage.miniMessage().deserialize(
      this.stalemateMessage,
      Placeholder.component("black_name", black.name()),
      Placeholder.component("white_name", white.name())
    );
  }
}
