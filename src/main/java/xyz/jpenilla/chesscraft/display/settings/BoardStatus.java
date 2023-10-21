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

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.config.Messages;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.display.BoardBoundUpdatingText;
import xyz.jpenilla.chesscraft.util.OptionTagResolver;
import xyz.jpenilla.chesscraft.util.Util;

@ConfigSerializable
public final class BoardStatus extends AbstractDisplaySettings<BoardBoundUpdatingText> {
  private List<String> gameText = List.of(
    "<b>Chess Board <board_name>",
    "<white>♚</white><white_displayname> <i><gray>vs</i> <black>♚</black><black_displayname>",
    "Current move: <current_move_color>♚</current_move_color><current_move_displayname>",
    "<time_control:'<white>♚</white><white_time> <gray>|</gray> <black>♚</black><black_time>':'<red>No time controls.'>"
  );
  private List<String> noGameText = List.of(
    "<b>Chess Board <board_name>",
    "<red>No active game."
  );
  private long updateRate = 10;

  public BoardStatus() {
    this.offset = new Vec3d(4, 4.5, -4);
  }

  @Override
  public DisplayType type() {
    return DisplayType.BOARD_STATUS;
  }

  @Override
  public BoardBoundUpdatingText getOrCreateState(final ChessCraft plugin, final ChessBoard board) {
    return new BoardBoundUpdatingText(
      plugin,
      board,
      board.loc().asVec3d().add(this.offset(board)).toLocation(board.world()),
      () -> this.text(board, board.hasGame()),
      this.updateRate,
      this
    );
  }

  private List<Component> text(final ChessBoard board, final boolean hasGame) {
    final TagResolver.Builder tags = TagResolver.builder();
    if (hasGame) {
      final ChessGame g = board.game();
      tags.resolver(Messages.playerTags(g.white(), "white", g.black(), "black", PieceColor.WHITE));
      tags.resolver(Messages.playerTags(g.player(g.nextMove()), "current_move", g.nextMove()));
      tags.resolver(new OptionTagResolver("time_control", g.time(g.white()) != null));
      if (g.time(g.white()) != null) {
        tags.resolvers(
          Placeholder.unparsed("white_time", g.time(g.white()).timeLeft()),
          Placeholder.unparsed("black_time", g.time(g.black()).timeLeft())
        );
      }
    }
    tags.resolver(Placeholder.parsed("board_name", board.name()));
    final TagResolver builtTags = tags.build();
    return (hasGame ? this.gameText : this.noGameText).stream().map(t -> MiniMessage.miniMessage().deserialize(t, builtTags)).toList();
  }

  @Override
  public void gameEnded(final BoardBoundUpdatingText state) {
    if (this.removeAfterGame()) {
      state.remove();
    } else {
      state.stopUpdates();
      // Schedule to ensure it runs after last update
      Util.schedule(state.plugin(), () -> state.text(this.text(state.board(), false)));
    }
  }
}
