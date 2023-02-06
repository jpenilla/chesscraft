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
  private String checkmate = "<winner_color>♚</winner_color><winner_displayname> <green>beat <loser_color>♚</loser_color><loser_displayname> by checkmate!";

  public Component checkmate(final ChessPlayer black, final ChessPlayer white, final PieceColor winner) {
    final ChessPlayer win = winner == PieceColor.BLACK ? black : white;
    final ChessPlayer loss = winner == PieceColor.BLACK ? white : black;
    return MiniMessage.miniMessage().deserialize(this.checkmate, winLoseTags(win, loss, winner));
  }

  private String stalemate = "<black>♚</black><black_displayname> <green>ended in a stalemate with <white>♚</white><white_displayname>!";

  public Component stalemate(final ChessPlayer black, final ChessPlayer white) {
    return MiniMessage.miniMessage().deserialize(
      this.stalemate,
      Placeholder.component("black_name", black.name()),
      Placeholder.component("white_name", white.name()),
      Placeholder.component("black_displayname", black.displayName()),
      Placeholder.component("white_displayname", white.displayName())
    );
  }

  private String forfeit = "<loser_color>♚</loser_color><loser_displayname> <green>forfeited to <winner_color>♚</winner_color><winner_displayname>!";

  public Component forfeit(final ChessPlayer black, final ChessPlayer white, final PieceColor forfeited) {
    final ChessPlayer win = forfeited == PieceColor.WHITE ? black : white;
    final ChessPlayer loss = forfeited == PieceColor.WHITE ? white : black;
    return MiniMessage.miniMessage().deserialize(this.forfeit, winLoseTags(win, loss, forfeited.other()));
  }

  private String boardAlreadyExists = "<red>A board with the name <name> already exists!</red> <white>Use <gray><hover_event:show_text:'<green>Click to run'><click_event:run_command:'/chess delete_board <name>'>/chess delete_board <name></gray> to delete it first if you want to replace it.";

  public Component boardAlreadyExists(final String name) {
    return MiniMessage.miniMessage().deserialize(this.boardAlreadyExists, name(name));
  }

  private String boardCreated = "<green>Successfully created board</green><gray>:</gray> <name>";

  public Component boardCreated(final String name) {
    return MiniMessage.miniMessage().deserialize(this.boardCreated, name(name));
  }

  private String boardDeleted = "<green>Successfully <red>deleted</red> board</green><gray>:</gray> <name>";

  public Component boardDeleted(final String name) {
    return MiniMessage.miniMessage().deserialize(this.boardDeleted, name(name));
  }

  private static TagResolver name(final String name) {
    return Placeholder.unparsed("name", name);
  }

  private String noSuchBoard = "No board exists with the name '<name>'";

  public Component noSuchBoard(final String name) {
    return MiniMessage.miniMessage().deserialize(this.noSuchBoard, name(name));
  }

  private static TagResolver[] winLoseTags(final ChessPlayer win, final ChessPlayer lose, final PieceColor winColor) {
    return new TagResolver[]{
      TagResolver.resolver("winner_color", Tag.styling(winColor.textColor())),
      TagResolver.resolver("loser_color", Tag.styling(winColor.other().textColor())),
      Placeholder.component("winner_name", win.name()),
      Placeholder.component("loser_name", lose.name()),
      Placeholder.component("winner_displayname", win.displayName()),
      Placeholder.component("loser_displayname", lose.displayName())
    };
  }
}
