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
import org.bukkit.entity.Player;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

@ConfigSerializable
public final class Messages {
  private String checkmate = "<winner_color>♚</winner_color><winner_displayname> <green>beat <loser_color>♚</loser_color><loser_displayname> by checkmate!";

  public Component checkmate(final ChessPlayer black, final ChessPlayer white, final PieceColor winner) {
    final ChessPlayer win = winner == PieceColor.BLACK ? black : white;
    final ChessPlayer loss = winner == PieceColor.BLACK ? white : black;
    return parse(this.checkmate, winLoseTags(win, loss, winner));
  }

  private String stalemate = "<black>♚</black><black_displayname> <green>ended in a stalemate with <white>♚</white><white_displayname>!";

  public Component stalemate(final ChessPlayer black, final ChessPlayer white) {
    return parse(this.stalemate, blackWhitePlayerTags(black, white));
  }

  private String forfeit = "<loser_color>♚</loser_color><loser_displayname> <green>forfeited to <winner_color>♚</winner_color><winner_displayname>!";

  public Component forfeit(final ChessPlayer black, final ChessPlayer white, final PieceColor forfeited) {
    final ChessPlayer win = forfeited == PieceColor.WHITE ? black : white;
    final ChessPlayer loss = forfeited == PieceColor.WHITE ? white : black;
    return parse(this.forfeit, winLoseTags(win, loss, forfeited.other()));
  }

  private String boardAlreadyExists = "<red>A board with the name <white><name></white> already exists!</red> <white>Use <gray><hover:show_text:'<green>Click to run'><click:run_command:'/chess delete_board <name>'>/chess delete_board <name></gray> to delete it first if you want to replace it.";

  public Component boardAlreadyExists(final String name) {
    return parse(this.boardAlreadyExists, name(name));
  }

  private String boardCreated = "<green>Successfully created board</green><gray>:</gray> <name>";

  public Component boardCreated(final String name) {
    return parse(this.boardCreated, name(name));
  }

  private String boardDeleted = "<green>Successfully <red>deleted</red> board</green><gray>:</gray> <name>";

  public Component boardDeleted(final String name) {
    return parse(this.boardDeleted, name(name));
  }

  private static TagResolver name(final String name) {
    return Placeholder.unparsed("name", name);
  }

  private String noSuchBoard = "No board exists with the name '<name>'";

  public Component noSuchBoard(final String name) {
    return parse(this.noSuchBoard, name(name));
  }

  private String boardOccupied = "<red>The <name> board is currently occupied!";

  public Component boardOccupied(final String name) {
    return parse(this.boardOccupied, name(name));
  }

  private String challengeSent = "<green>Challenge has been sent! <opponent_displayname> has 30 seconds to accept.";

  public Component challengeSent(final ChessPlayer player, final ChessPlayer opponent, final PieceColor playerColor) {
    return parse(this.challengeSent, playerTags(player, "player", opponent, "opponent", playerColor));
  }

  private String challengeReceived = "<green>You have been challenged to Chess by <challenger_displayname>! They chose to be <challenger_color><challenger_color_name></challenger_color>. Type <white><click:run_command:'/chess accept'><hover:show_text:'<green>Click to run'>/chess accept</white> to accept. Challenge expires in 30 seconds.";

  public Component challengeReceived(final ChessPlayer challenger, final ChessPlayer player, final PieceColor challengerColor) {
    return parse(this.challengeReceived, playerTags(challenger, "challenger", player, "player", challengerColor));
  }

  private String noChallengeToAccept = "<red>No challenge to accept!";

  public Component noChallengeToAccept() {
    return parse(this.noChallengeToAccept);
  }

  private String alreadyInGame = "<red>You are already in an active match.";

  public Component alreadyInGame() {
    return parse(this.alreadyInGame);
  }

  private String opponentAlreadyInGame = "<red><opponent_displayname> is already in an active match.";

  public Component opponentAlreadyInGame(final Player opponent) {
    return parse(
      this.opponentAlreadyInGame,
      Placeholder.component("opponent_name", opponent.name()),
      Placeholder.component("opponent_displayname", opponent.displayName())
    );
  }

  private String matchStarted = "<green>Match has started!";

  public Component matchStarted(final ChessBoard board, final ChessPlayer white, final ChessPlayer black) {
    return parse(this.matchStarted, blackWhitePlayerTags(black, white), Placeholder.unparsed("board", board.name()));
  }

  private String mustBeInMatch = "<red>You must be in a match to use this command.";

  public Component mustBeInMatch() {
    return parse(this.mustBeInMatch);
  }

  private String nextPromotionSet = "<green>Your next pawn to reach the 1/8 rank will promote to <type>, instead of the default QUEEN.";

  public Component nextPromotionSet(final PieceType type) {
    return parse(this.nextPromotionSet, Placeholder.unparsed("type", type.toString()));
  }

  private String cpuThinking = "<italic><gray>CPU is thinking...";

  public Component cpuThinking() {
    return parse(this.cpuThinking);
  }

  private String madeMove = "<move_color>♚</move_color><move_displayname><gray>:</gray> <move>";

  public Component madeMove(final ChessPlayer mover, final ChessPlayer opponent, final PieceColor moverColor, final String move) {
    return parse(this.madeMove, playerTags(mover, "move", opponent, "opponent", moverColor), Placeholder.unparsed("move", move));
  }

  private String notInThisGame = "<red>You are not a player in this match.";

  public Component notInThisGame() {
    return parse(this.notInThisGame);
  }

  private String notYourMove = "<red>Not your move.";

  public Component notYourMove() {
    return parse(this.notYourMove);
  }

  private String notYourPiece = "<red>Not your piece.";

  public Component notYourPiece() {
    return parse(this.notYourPiece);
  }

  private String invalidMove = "<red>Invalid move.";

  public Component invalidMove() {
    return parse(this.invalidMove);
  }

  private static TagResolver blackWhitePlayerTags(final ChessPlayer black, final ChessPlayer white) {
    return playerTags(black, "black", white, "white", PieceColor.BLACK);
  }

  private static TagResolver winLoseTags(final ChessPlayer win, final ChessPlayer lose, final PieceColor winColor) {
    return playerTags(win, "winner", lose, "loser", winColor);
  }

  private static TagResolver playerTags(
    final ChessPlayer p1,
    final String p1prefix,
    final ChessPlayer p2,
    final String p2prefix,
    final PieceColor p1Color
  ) {
    return TagResolver.resolver(
      TagResolver.resolver(p1prefix + "_color", Tag.styling(p1Color.textColor())),
      TagResolver.resolver(p2prefix + "_color", Tag.styling(p1Color.other().textColor())),
      Placeholder.unparsed(p1prefix + "_color_name", p1Color.toString()),
      Placeholder.unparsed(p2prefix + "_color_name", p1Color.other().toString()),
      Placeholder.component(p1prefix + "_name", p1.name()),
      Placeholder.component(p2prefix + "_name", p2.name()),
      Placeholder.component(p1prefix + "_displayname", p1.displayName()),
      Placeholder.component(p2prefix + "_displayname", p2.displayName())
    );
  }

  private static Component parse(final String input, final TagResolver... tagResolvers) {
    return MiniMessage.miniMessage().deserialize(input, tagResolvers);
  }
}
