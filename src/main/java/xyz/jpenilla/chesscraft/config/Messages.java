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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.db.Database;
import xyz.jpenilla.chesscraft.util.OptionTagResolver;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

@ConfigSerializable
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public final class Messages {
  private String checkmate = "<winner_color>♚</winner_color><winner_displayname> <green>beat <loser_color>♚</loser_color></green><loser_displayname><green> by checkmate!";

  public Component checkmate(final ChessPlayer black, final ChessPlayer white, final PieceColor winner) {
    final ChessPlayer win = winner == PieceColor.BLACK ? black : white;
    final ChessPlayer loss = winner == PieceColor.BLACK ? white : black;
    return parse(this.checkmate, winLoseTags(win, loss, winner));
  }

  private String stalemate = "<black>♚</black><black_displayname> <green>ended in a stalemate with <white>♚</white></green><white_displayname><green>!";

  public Component stalemate(final ChessPlayer black, final ChessPlayer white) {
    return parse(this.stalemate, blackWhitePlayerTags(black, white));
  }

  private String drawByRepetition = "<black>♚</black><black_displayname> <green>ended in a draw by repetition with <white>♚</white></green><white_displayname><green>!";

  public Component drawByRepetition(final ChessPlayer black, final ChessPlayer white) {
    return parse(this.drawByRepetition, blackWhitePlayerTags(black, white));
  }

  private String drawByFiftyMoveRule = "<black>♚</black><black_displayname> <green>ended in a draw by the fifty move rule with <white>♚</white></green><white_displayname><green>!";

  public Component drawByFiftyMoveRule(final ChessPlayer black, final ChessPlayer white) {
    return parse(this.drawByFiftyMoveRule, blackWhitePlayerTags(black, white));
  }

  private String forfeit = "<loser_color>♚</loser_color><loser_displayname> <green>forfeited to <winner_color>♚</winner_color></green><winner_displayname><green>!";

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
    return unparsed("name", name);
  }

  private String noSuchBoard = "No board exists with the name '<name>'";

  public Component noSuchBoard(final String name) {
    return parse(this.noSuchBoard, name(name));
  }

  private String noMatchToCancel = "<red>No match to cancel at board '<name>'.";

  public Component noMatchToCancel(final String name) {
    return parse(this.noMatchToCancel, name(name));
  }

  private String boardOccupied = "<red>The <name> board is currently occupied!";

  public Component boardOccupied(final String name) {
    return parse(this.boardOccupied, name(name));
  }

  private String challengeSent = "<green>Challenge has been sent! </green><opponent_displayname><green> has 30 seconds to accept.";

  public Component challengeSent(final ChessPlayer player, final ChessPlayer opponent, final PieceColor playerColor) {
    return parse(this.challengeSent, playerOpponentTags(player, opponent, playerColor));
  }

  private String challengeReceived = "<green>You have been challenged to Chess by </green><challenger_displayname><green>! They chose to be <challenger_color><challenger_color_name></challenger_color>.\n" +
    "Time controls<gray>:</gray> <white><time_control></white>\n" +
    "Type <white><click:run_command:'/chess accept'><hover:show_text:'<green>Click to run'>/chess accept</white> to accept. Challenge expires in 30 seconds.";

  private String noTimeControls = "None";

  public Component challengeReceived(final ChessPlayer challenger, final ChessPlayer player, final PieceColor challengerColor, final @Nullable TimeControlSettings timeControl) {
    return parse(
      this.challengeReceived,
      challengerPlayerTags(challenger, player, challengerColor),
      timeControl != null ? unparsed("time_control", timeControl.toString()) : component("time_control", parse(this.noTimeControls))
    );
  }

  private String noPendingChallenge = "<red>You do not have an incoming challenge!";

  public Component noPendingChallenge() {
    return parse(this.noPendingChallenge);
  }

  private String challengeDenied = "<opponent_displayname><red> denied your challenge.";

  public Component challengeDenied(final ChessPlayer player, final ChessPlayer opponent, final PieceColor playerColor) {
    return parse(this.challengeDenied, playerOpponentTags(player, opponent, playerColor));
  }

  private String challengeDeniedFeedback = "<green>Denied </green><challenger_displayname>'s <green>challenge.";

  public Component challengeDeniedFeedback(final ChessPlayer challenger, final ChessPlayer player, final PieceColor challengerColor) {
    return parse(this.challengeDeniedFeedback, challengerPlayerTags(challenger, player, challengerColor));
  }

  private String alreadyInGame = "<red>You are already in an active match.";

  public Component alreadyInGame() {
    return parse(this.alreadyInGame);
  }

  private String opponentAlreadyInGame = "<opponent_displayname><red> is already in an active match.";

  public Component opponentAlreadyInGame(final Player opponent) {
    return parse(
      this.opponentAlreadyInGame,
      component("opponent_name", opponent.name()),
      component("opponent_displayname", opponent.displayName())
    );
  }

  private String matchStarted = "<green>Match has started!";

  public Component matchStarted(final ChessBoard board, final ChessPlayer white, final ChessPlayer black) {
    return parse(this.matchStarted, blackWhitePlayerTags(black, white), unparsed("board", board.name()));
  }

  private String matchResumed = "<green>Match has resumed!";

  public Component matchResumed(final ChessBoard board, final ChessPlayer white, final ChessPlayer black) {
    return parse(this.matchResumed, blackWhitePlayerTags(black, white), unparsed("board", board.name()));
  }

  private String mustBeInMatch = "<red>You must be in a match to use this command.";

  public Component mustBeInMatch() {
    return parse(this.mustBeInMatch);
  }

  private String nextPromotionSet = "<green>Your next pawn to reach the 1/8 rank will promote to <type>, instead of the default QUEEN.";

  public Component nextPromotionSet(final PieceType type) {
    return parse(this.nextPromotionSet, unparsed("type", type.toString()));
  }

  private String cpuThinking = "<italic><cpu_color>♚</cpu_color><gray>CPU is thinking...";

  public Component cpuThinking(final PieceColor color) {
    return parse(this.cpuThinking, TagResolver.resolver("cpu_color", Tag.styling(color.textColor())));
  }

  private String madeMove = "<player_color>♚</player_color><player_displayname><gray>:</gray> <move>";

  public Component madeMove(final ChessPlayer mover, final ChessPlayer opponent, final PieceColor moverColor, final String move) {
    return parse(this.madeMove, playerOpponentTags(mover, opponent, moverColor), unparsed("move", move));
  }

  private String notInThisGame = "<red>You are not a player in this match.";

  public Component notInThisGame() {
    return parse(this.notInThisGame);
  }

  private String notYourMove = "<red>Not your move.";

  public Component notYourMove() {
    return parse(this.notYourMove);
  }

  private String chessEngineProcessing = "<red>Chess engine is currently processing, please try again shortly.";

  public Component chessEngineProcessing() {
    return parse(this.chessEngineProcessing);
  }

  private String notYourPiece = "<red>Not your piece.";

  public Component notYourPiece() {
    return parse(this.notYourPiece);
  }

  private String invalidMove = "<red>Invalid move.";

  public Component invalidMove() {
    return parse(this.invalidMove);
  }

  private String showingLegalMoves = "Highlighting of legal moves<gray>:</gray> <on_off>";

  public Component showingLegalMoves(final boolean value) {
    return parse(this.showingLegalMoves, this.onOff(value));
  }

  private String timeDisplay = "<opponent_color>♚</opponent_color><opponent_time> <gray>|</gray> <player_color>♚</player_color><player_time>";

  public Component timeDisplay(final ChessGame game, final PieceColor playerColor) {
    final ChessPlayer player = game.player(playerColor);
    final ChessPlayer opp = game.player(playerColor.other());
    return parse(
      this.timeDisplay,
      playerOpponentTags(player, opp, playerColor),
      unparsed("player_time", game.time(player).timeLeftString()),
      unparsed("opponent_time", game.time(opp).timeLeftString())
    );
  }

  private String invalidTimeControl = "Invalid time control '<input>', expected format is '<time>[:<increment>]'";

  public Component invalidTimeControl(final String input) {
    return parse(this.invalidTimeControl, unparsed("input", input));
  }

  private String ranOutOfTime = "<player_color>♚</player_color><player_displayname> ran out of time!";

  public Component ranOutOfTime(final ChessGame game, final PieceColor playerColor) {
    final ChessPlayer player = game.player(playerColor);
    final ChessPlayer opp = game.player(playerColor.other());
    return parse(this.ranOutOfTime, playerOpponentTags(player, opp, playerColor));
  }

  private String resetBoard = "<green>Successfully reset board '<name>'.";

  public Component resetBoard(final ChessBoard board) {
    return parse(this.resetBoard, name(board.name()));
  }

  private String matchCancelled = "<red>Match cancelled.";

  public Component matchCancelled() {
    return parse(this.matchCancelled);
  }

  private String pauseProposedSender = "<opponent_color>♚</opponent_color><opponent_displayname> has been notified that you wish to pause the match.";

  public Component pauseProposedSender(final ChessGame game, final PieceColor playerColor) {
    final ChessPlayer player = game.player(playerColor);
    final ChessPlayer opp = game.player(playerColor.other());
    return parse(this.pauseProposedSender, playerOpponentTags(player, opp, playerColor));
  }

  private String pauseProposedRecipient = "<hover:show_text:'Click to accept'><click:run_command:'/chess accept_pause'><opponent_color>♚</opponent_color><opponent_displayname> has proposed a pause on the match. Use <white>/chess accept_pause</white> to accept.";

  public Component pauseProposedRecipient(final ChessGame game, final PieceColor playerColor) {
    final ChessPlayer player = game.player(playerColor);
    final ChessPlayer opp = game.player(playerColor.other());
    return parse(this.pauseProposedRecipient, playerOpponentTags(player, opp, playerColor));
  }

  private String pausedMatch = "<green>Match has been paused.";

  public Component pausedMatch() {
    return parse(this.pausedMatch);
  }

  private String noPauseProposed = "<red>Your match does not have a pause proposed. Use /chess pause_match to propose one.";

  public Component noPauseProposed() {
    return parse(this.noPauseProposed);
  }

  private String opponentOffline = "<red><opponent_name> is not online!";

  public Component opponentOffline(final ChessPlayer opponent) {
    return parse(this.opponentOffline, component("opponent_displayname", opponent.displayName()), component("opponent_name", opponent.name()));
  }

  private String noPausedMatch = "<red>There is no paused match with the id <match_id>";

  public Component noPausedMatch(final UUID uuid) {
    return parse(this.noPausedMatch, parsed("match_id", uuid.toString()));
  }

  private String noPausedMatches = "<red>No paused matches found.";

  public Component noPausedMatches() {
    return parse(this.noPausedMatches);
  }

  private String noCompleteMatches = "<red>No complete matches found.";

  public Component noCompleteMatches() {
    return parse(this.noCompleteMatches);
  }

  private String youAreNotInThisMatch = "<red>You are not a player in that match.";

  public Component youAreNotInThisMatch() {
    return parse(this.youAreNotInThisMatch);
  }

  private String pausedMatchInfo = "<click:suggest_command:'/chess resume_match <match_id> '><white>♚</white><white_displayname> <i><gray>vs</i> <black>♚</black><black_displayname> <gray><i><time>";

  public Component pausedMatchInfo(final Database db, final GameState state) {
    return parse(this.pausedMatchInfo, blackWhitePlayerTags(state.blackOffline(db).join(), state.whiteOffline(db).join()), parsed("match_id", state.id().toString()), this.timeTag(state));
  }

  private String completeMatchInfo = "<white>♚</white><white_displayname> <i><gray>vs</i> <black>♚</black><black_displayname> <result> <gray><i><time>";

  public Component completeMatchInfo(final Database db, final GameState state) {
    return parse(
      this.completeMatchInfo,
      blackWhitePlayerTags(state.blackOffline(db).join(), state.whiteOffline(db).join()),
      component("result", requireNonNull(state.result(), "result").describe(this)),
      this.timeTag(state)
    );
  }

  private TagResolver timeTag(final GameState state) {
    final Timestamp timestamp = requireNonNull(state.lastUpdated(), "lastUpdated");
    final LocalDateTime localDateTime = timestamp.toLocalDateTime();
    final int hour = localDateTime.getHour();

    return component(
      "time",
      this.timestamp(
        localDateTime.getYear(),
        localDateTime.getMonthValue(),
        localDateTime.getDayOfMonth(),
        hour > 12 ? hour - 12 : hour,
        hour,
        localDateTime.getMinute(),
        hour <= 11
      )
    );
  }

  private String pausedMatchesHeader = "<green><bold>Paused matches for <displayname>";

  public Component pausedMatchesHeader(final Component username, final Component displayName) {
    return parse(this.pausedMatchesHeader, component("username", username), component("displayname", displayName));
  }

  private String matchHistoryHeader = "<green><bold>Match history for <displayname>";

  public Component matchHistoryHeader(final Component username, final Component displayName) {
    return parse(this.matchHistoryHeader, component("username", username), component("displayname", displayName));
  }

  private String clickForPreviousPage = "Click for previous page";

  public Component clickForPreviousPage() {
    return parse(this.clickForPreviousPage);
  }

  private String clickForNextPage = "Click for next page";

  public Component clickForNextPage() {
    return parse(this.clickForNextPage);
  }

  private String pageOutOfRange = "<red>Page <page> is out of range! There are only <pages> pages.";

  public Component pageOutOfRange(final int page, final int pages) {
    return parse(this.pageOutOfRange, parsed("page", String.valueOf(page)), parsed("pages", String.valueOf(pages)));
  }

  private String paginationFooter = "<gray>Page <page><white>/</white><pages> <aqua><buttons>";

  public Component paginationFooter(final int page, final int pages, final ComponentLike buttons) {
    return parse(
      this.paginationFooter,
      parsed("page", String.valueOf(page)),
      parsed("pages", String.valueOf(pages)),
      component("buttons", buttons)
    );
  }

  private String nonPlayerMustProvidePlayer = "<red>Must specify the player.";

  public Component nonPlayerMustProvidePlayer() {
    return parse(this.nonPlayerMustProvidePlayer);
  }

  private String timestamp = "<month>/<day>/<year_short> <hour>:<minute><am:AM:PM>";

  public Component timestamp(
    final int year,
    final int month,
    final int day,
    final int hour,
    final int hour24,
    final int minute,
    final boolean am
  ) {
    return parse(
      this.timestamp,
      parsed("year", String.valueOf(year)),
      parsed("year_short", String.valueOf(year).substring(2)),
      parsed("month", String.valueOf(month)),
      parsed("day", String.valueOf(day)),
      parsed("hour", String.valueOf(hour)),
      parsed("hour_military", String.valueOf(hour24)),
      parsed("minute", String.format("%02d", minute)),
      new OptionTagResolver("am", am)
    );
  }

  private String resultWin = "<winner_color>♚</winner_color>Checkmate";

  public Component resultWin(final PieceColor winner) {
    return parse(this.resultWin, Placeholder.styling("winner_color", winner.textColor()), Placeholder.styling("loser_color", winner.other().textColor()));
  }

  private String resultStalemate = "Stalemate";

  public Component resultStalemate() {
    return parse(this.resultStalemate);
  }

  private String resultDrawByRepetition = "Draw by repetition";

  public Component resultDrawByRepetition() {
    return parse(this.resultDrawByRepetition);
  }

  private String resultDrawByFiftyMoveRule = "Draw by 50 move rule";

  public Component resultDrawByFiftyMoveRule() {
    return parse(this.resultDrawByFiftyMoveRule);
  }

  private String resultForfeit = "<forfeit_color>♚</forfeit_color>Forfeit";

  public Component resultForfeit(final PieceColor forfeited) {
    return parse(this.resultForfeit, Placeholder.styling("forfeit_color", forfeited.textColor()), Placeholder.styling("winner_color", forfeited.other().textColor()));
  }

  private String on = "<green>On";

  public Component on() {
    return parse(this.on);
  }

  private String off = "<red>Off";

  public Component off() {
    return parse(this.off);
  }

  private TagResolver onOff(final boolean value) {
    if (value) {
      return component("on_off", this.on());
    }
    return component("on_off", this.off());
  }

  private static TagResolver playerOpponentTags(final ChessPlayer player, final ChessPlayer opponent, final PieceColor playerColor) {
    return playerTags(player, "player", opponent, "opponent", playerColor);
  }

  private static TagResolver challengerPlayerTags(final ChessPlayer challenger, final ChessPlayer player, final PieceColor challengerColor) {
    return playerTags(challenger, "challenger", player, "player", challengerColor);
  }

  private static TagResolver blackWhitePlayerTags(final ChessPlayer black, final ChessPlayer white) {
    return playerTags(black, "black", white, "white", PieceColor.BLACK);
  }

  private static TagResolver winLoseTags(final ChessPlayer win, final ChessPlayer lose, final PieceColor winColor) {
    return playerTags(win, "winner", lose, "loser", winColor);
  }

  public static TagResolver playerTags(
    final ChessPlayer p1,
    final String p1prefix,
    final ChessPlayer p2,
    final String p2prefix,
    final PieceColor p1Color
  ) {
    return TagResolver.resolver(
      playerTags(p1, p1prefix, p1Color),
      playerTags(p2, p2prefix, p1Color.other())
    );
  }

  public static TagResolver playerTags(
    final ChessPlayer player,
    final String prefix,
    final PieceColor color
  ) {
    return TagResolver.resolver(
      TagResolver.resolver(prefix + "_color", Tag.styling(color.textColor())),
      unparsed(prefix + "_color_name", color.toString()),
      component(prefix + "_name", player.name()),
      component(prefix + "_displayname", player.displayName())
    );
  }

  private static Component parse(final String input, final TagResolver... tagResolvers) {
    return MiniMessage.miniMessage().deserialize(input, tagResolvers);
  }
}
