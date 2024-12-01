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
package xyz.jpenilla.chesscraft.command;

import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.internal.BukkitBrigadierMapper;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.flag.FlagContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.translations.LocaleExtractor;
import org.incendo.cloud.type.tuple.Pair;
import xyz.jpenilla.chesscraft.BoardManager;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.command.parser.ChessBoardParser;
import xyz.jpenilla.chesscraft.command.parser.TimeControlParser;
import xyz.jpenilla.chesscraft.config.Messages;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.PVPChallenge;
import xyz.jpenilla.chesscraft.data.PVPChallengeImpl;
import xyz.jpenilla.chesscraft.data.PVPChallengeResumeMatchImpl;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.util.Elo;
import xyz.jpenilla.chesscraft.util.MatchExporter;
import xyz.jpenilla.chesscraft.util.Pagination;
import xyz.jpenilla.chesscraft.util.PaginationHelper;
import xyz.jpenilla.chesscraft.util.Permissions;

import static org.incendo.cloud.bukkit.parser.MaterialParser.materialParser;
import static org.incendo.cloud.bukkit.parser.OfflinePlayerParser.offlinePlayerParser;
import static org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser.singlePlayerSelectorParser;
import static org.incendo.cloud.exception.handling.ExceptionHandler.unwrappingHandler;
import static org.incendo.cloud.key.CloudKey.cloudKey;
import static org.incendo.cloud.parser.standard.EnumParser.enumParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static org.incendo.cloud.parser.standard.UUIDParser.uuidParser;
import static org.incendo.cloud.suggestion.SuggestionProvider.blockingStrings;
import static org.incendo.cloud.translations.TranslationBundle.core;
import static org.incendo.cloud.translations.bukkit.BukkitTranslationBundle.bukkit;
import static org.incendo.cloud.translations.minecraft.extras.AudienceLocaleExtractor.audienceLocaleExtractor;
import static org.incendo.cloud.translations.minecraft.extras.MinecraftExtrasTranslationBundle.minecraftExtras;
import static org.incendo.cloud.type.range.Range.intRange;
import static xyz.jpenilla.chesscraft.command.parser.ChessBoardParser.chessBoardParser;
import static xyz.jpenilla.chesscraft.command.parser.PromotionParser.promotionParser;
import static xyz.jpenilla.chesscraft.command.parser.TimeControlParser.timeControlParser;

public final class Commands {
  public static final CloudKey<ChessCraft> PLUGIN = cloudKey("chesscraft", ChessCraft.class);

  private final ChessCraft plugin;
  private final LegacyPaperCommandManager<CommandSender> mgr;
  private final BoardManager boardManager;
  private final PaginationHelper pagination;

  public Commands(final ChessCraft plugin) {
    this.plugin = plugin;
    this.boardManager = plugin.boardManager();
    this.mgr = createCommandManager(plugin);
    this.pagination = new PaginationHelper(plugin);
  }

  public void register() {
    new HelpCommand(this.mgr).register();

    final Command.Builder<CommandSender> chess = this.mgr.commandBuilder("chess");

    this.mgr.command(chess.literal("version")
      .permission(Permissions.COMMAND_VERSION)
      .handler(this::version));

    this.mgr.command(chess.literal("boards")
      .permission(Permissions.COMMAND_BOARDS)
      .handler(this::boards));

    this.mgr.command(chess.literal("reload")
      .permission(Permissions.COMMAND_RELOAD)
      .handler(this::reload));

    this.mgr.command(chess.literal("create_board")
      .required("name", stringParser())
      .required("facing", enumParser(CardinalDirection.class))
      .optional("scale", integerParser(1), DefaultValue.constant(1))
      .senderType(Player.class)
      .permission(Permissions.COMMAND_CREATE_BOARD)
      .handler(this::createBoard));

    this.mgr.command(chess.literal("set_checkerboard")
      .required("board", chessBoardParser())
      .flag(this.mgr.flagBuilder("black").withComponent(materialParser()))
      .flag(this.mgr.flagBuilder("white").withComponent(materialParser()))
      .flag(this.mgr.flagBuilder("border").withComponent(materialParser()))
      .permission(Permissions.COMMAND_SET_CHECKERBOARD)
      .handler(this::setCheckerboard));

    this.mgr.command(chess.literal("delete_board")
      .required("board", chessBoardParser())
      .permission(Permissions.COMMAND_DELETE_BOARD)
      .handler(this::deleteBoard));

    this.mgr.command(chess.literal("challenge")
      .literal("cpu")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.PLAYABLE_ONLY))
      .required("color", enumParser(PieceColor.class))
      .argument(eloComponent("cpu_elo"))
      .optional("time_control", timeControlParser())
      .senderType(Player.class)
      .permission(Permissions.COMMAND_CHALLENGE_CPU)
      .handler(this::challengeCpu));

    this.mgr.command(chess.literal("challenge")
      .literal("player")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.PLAYABLE_ONLY))
      .required("player", singlePlayerSelectorParser())
      .required("color", enumParser(PieceColor.class))
      .optional("time_control", timeControlParser())
      .senderType(Player.class)
      .permission(Permissions.COMMAND_CHALLENGE_PLAYER)
      .handler(this::challengePlayer));

    this.mgr.command(chess.literal("accept")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_ACCEPT)
      .handler(this::accept));

    this.mgr.command(chess.literal("deny")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_DENY)
      .handler(this::deny));

    this.mgr.command(chess.literal("next_promotion")
      .required("type", promotionParser())
      .senderType(Player.class)
      .permission(Permissions.COMMAND_NEXT_PROMOTION)
      .handler(this::nextPromotion));

    this.mgr.command(chess.literal("show_legal_moves")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_SHOW_LEGAL_MOVES)
      .handler(this::showLegalMoves));

    this.mgr.command(chess.literal("forfeit")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_FORFEIT)
      .handler(this::forfeit));

    this.mgr.command(chess.literal("reset_board")
      .required("board", chessBoardParser())
      .flag(CommandFlag.builder("clear"))
      .permission(Permissions.COMMAND_RESET_BOARD)
      .handler(this::resetBoard));

    this.mgr.command(chess.literal("cpu_match")
      .required("board", chessBoardParser())
      .flag(this.mgr.flagBuilder("white_elo").withAliases("w").withComponent(eloComponent("elo")))
      .flag(this.mgr.flagBuilder("black_elo").withAliases("b").withComponent(eloComponent("elo")))
      .flag(this.mgr.flagBuilder("move_delay").withAliases("d").withComponent(integerParser(0)))
      .flag(this.mgr.flagBuilder("time_control").withAliases("t").withComponent(timeControlParser()))
      .flag(this.mgr.flagBuilder("replace").withAliases("r"))
      .permission(Permissions.COMMAND_CPU_MATCH)
      .handler(this::cpuMatch));

    this.mgr.command(chess.literal("cancel_match")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.OCCUPIED_ONLY))
      .permission(Permissions.COMMAND_CANCEL_MATCH)
      .handler(this::cancelMatch));

    this.mgr.command(chess.literal("pause_match")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_PAUSE_MATCH)
      .handler(this::pauseMatch));

    this.mgr.command(chess.literal("accept_pause")
      .senderType(Player.class)
      .permission(Permissions.COMMAND_ACCEPT_PAUSE)
      .handler(this::acceptPause));

    this.mgr.command(chess.literal("resume_match")
      .required("id", uuidParser())
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.PLAYABLE_ONLY))
      .senderType(Player.class)
      .permission(Permissions.COMMAND_RESUME_MATCH)
      .futureHandler(this::resumeMatch));

    final Consumer<Command.Builder<? extends CommandSender>> withPage = builder -> {
      this.mgr.command(builder);
      this.mgr.command(builder.literal("page").required("page", integerParser(1)));
    };

    final Command.Builder<CommandSender> pausedMatches = chess.literal("paused_matches")
      .futureHandler(this::pausedMatches);
    withPage.accept(pausedMatches.permission(Permissions.COMMAND_PAUSED_MATCHES_SELF));
    withPage.accept(pausedMatches.permission(Permissions.COMMAND_PAUSED_MATCHES_OTHERS)
      .required("player", offlinePlayerParser()));

    final Command.Builder<CommandSender> matchHistory = chess.literal("match_history")
      .futureHandler(this::matchHistory);
    withPage.accept(matchHistory.permission(Permissions.COMMAND_MATCH_HISTORY_SELF));
    withPage.accept(matchHistory.permission(Permissions.COMMAND_MATCH_HISTORY_OTHERS)
      .required("player", offlinePlayerParser()));

    this.mgr.command(chess.literal("export_match")
      .required("id", uuidParser())
      .permission(Permissions.COMMAND_EXPORT_MATCH)
      .senderType(Player.class)
      .futureHandler(this::exportMatch));

    this.mgr.command(chess.literal("leaderboard")
      .permission(Permissions.COMMAND_LEADERBOARD)
      .futureHandler(this::leaderboard));
  }

  private static CommandComponent.Builder<CommandSender, Integer> eloComponent(final String name) {
    return CommandComponent.<CommandSender, Integer>builder()
      .valueType(Integer.class)
      .name(name)
      .parser(integerParser(100, 4000))
      .suggestionProvider(blockingStrings((ctx, input) -> {
        if (input.remainingInput().length() <= 1) {
          return List.of("800", "1200", "1400", "1600", "1800", "2000", "2200", "2400");
        }
        return IntegerParser.getSuggestions(intRange(100, 4000), input);
      }));
  }

  private void version(final CommandContext<CommandSender> ctx) {
    ctx.sender().sendRichMessage("<bold><black>Chess<white>Craft");
    ctx.sender().sendRichMessage("<gray><italic>  v" + this.plugin.getPluginMeta().getVersion());
  }

  private void boards(final CommandContext<CommandSender> ctx) {
    final CommandSender sender = ctx.sender();
    sender.sendMessage("Chess boards:");
    for (final ChessBoard board : this.boardManager.boards()) {
      ctx.sender().sendMessage(Component.text().content(board.name()).apply(builder -> {
        if (board.hasGame()) {
          builder.append(Component.text(": ", NamedTextColor.GRAY))
            .append(board.game().white().name().color(NamedTextColor.WHITE))
            .append(Component.text(" vs ", NamedTextColor.GREEN, TextDecoration.ITALIC))
            .append(board.game().black().name().color(NamedTextColor.DARK_GRAY));
        }
      }));
    }
  }

  private void reload(final CommandContext<CommandSender> ctx) {
    ctx.sender().sendRichMessage("<gray><italic>Reloading configs... This will pause any active matches.");
    this.plugin.reloadMainConfig();
    this.boardManager.reload();
    ctx.sender().sendRichMessage("<green>Reloaded configs.");
  }

  private void createBoard(final CommandContext<Player> ctx) {
    final String name = ctx.get("name");
    final Player sender = ctx.sender();
    if (this.boardManager.board(name) != null) {
      sender.sendMessage(this.messages().boardAlreadyExists(name));
      return;
    }
    this.boardManager.createBoard(
      name,
      sender.getWorld(),
      Vec3i.fromBlockLocation(sender.getLocation()),
      ctx.get("facing"),
      ctx.get("scale")
    );
    sender.sendMessage(this.messages().boardCreated(name));
  }

  private void setCheckerboard(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    final FlagContext flags = ctx.flags();
    board.applyCheckerboard(
      flags.<Material>getValue("black").orElse(Material.BLACK_CONCRETE),
      flags.<Material>getValue("white").orElse(Material.WHITE_CONCRETE),
      flags.get("border")
    );
    ctx.sender().sendRichMessage("<green>Set blocks to world.");
  }

  private void challengeCpu(final CommandContext<Player> ctx) {
    final ChessBoard board = ctx.get("board");
    final Player sender = ctx.sender();
    if (this.boardManager.inGame(sender)) {
      sender.sendMessage(this.messages().alreadyInGame());
      return;
    }
    if (canCancelCpuMatch(board)) {
      this.cancelCpuMatch(board);
    }
    if (board.hasGame() || board.autoCpuGame().cpuGamesOnly()) {
      sender.sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    final PieceColor userColor = ctx.get("color");
    final ChessPlayer user = ChessPlayer.player(sender);
    final ChessPlayer cpu = ChessPlayer.cpu(ctx.getOrDefault("cpu_elo", 800));
    board.startGame(
      userColor == PieceColor.WHITE ? user : cpu,
      userColor == PieceColor.BLACK ? user : cpu,
      ctx.<TimeControlSettings>optional("time_control").orElse(null)
    );
  }

  private void challengePlayer(final CommandContext<Player> ctx) {
    final ChessBoard board = ctx.get("board");
    final Player sender = ctx.sender();
    final Player opponent = Objects.requireNonNull(ctx.<SinglePlayerSelector>get("player").single());
    if (sender.equals(opponent)) {
      ctx.sender().sendRichMessage("<red>You cannot challenge yourself.");
      return;
    } else if (this.boardManager.inGame(sender)) {
      sender.sendMessage(this.messages().alreadyInGame());
      return;
    } else if (this.boardManager.inGame(opponent)) {
      sender.sendMessage(this.messages().opponentAlreadyInGame(opponent));
      return;
    } else if (canCancelCpuMatch(board)) {
      this.cancelCpuMatch(board);
    } else if (board.hasGame() || board.autoCpuGame().cpuGamesOnly() || this.boardManager.challenges().asMap().values().stream().anyMatch(c -> c.board() == board)) {
      sender.sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    final PieceColor userColor = ctx.get("color");
    final ChessPlayer user = ChessPlayer.player(sender);
    final ChessPlayer opp = ChessPlayer.player(opponent);
    final @Nullable TimeControlSettings timeControl = ctx.<TimeControlSettings>optional("time_control").orElse(null);
    this.boardManager.challenges().put(
      opponent.getUniqueId(),
      new PVPChallengeImpl(board, sender, opponent, userColor, timeControl)
    );
    sender.sendMessage(this.messages().challengeSent(user, opp, userColor));
    opponent.sendMessage(this.messages().challengeReceived(user, opp, userColor, timeControl));
  }

  private void cancelCpuMatch(final ChessBoard board) {
    this.boardManager.delayAutoCpu(board);
    final ChessGame game = board.game();
    board.endGameAndWait();
    game.audience().sendMessage(this.messages().matchCancelled());
  }

  private static boolean canCancelCpuMatch(final ChessBoard board) {
    return board.hasGame() && board.game().cpuVsCpu() && board.autoCpuGame().enabled && board.autoCpuGame().allowPlayerUse;
  }

  private void accept(final CommandContext<Player> ctx) {
    final @Nullable PVPChallenge challenge = this.pollChallenge(ctx);
    if (challenge == null) {
      return;
    }
    if (this.boardManager.inGame(challenge.challenger())) {
      ctx.sender().sendMessage(this.messages().opponentAlreadyInGame(challenge.challenger()));
      return;
    }

    if (challenge instanceof PVPChallenge.ResumeMatch resume) {
      resume.board().resumeGame(resume.state());
    } else {
      challenge.board().startGame(challenge.white(), challenge.black(), challenge.timeControl());
    }
  }

  private void deny(final CommandContext<Player> ctx) {
    final @Nullable PVPChallenge challenge = this.pollChallenge(ctx);
    if (challenge == null) {
      ctx.sender().sendMessage(this.messages().noPendingChallenge());
      return;
    }
    challenge.challenger().sendMessage(this.messages().challengeDenied(
      ChessPlayer.player(challenge.challenger()),
      ChessPlayer.player(challenge.player()),
      challenge.challengerColor()
    ));
    ctx.sender().sendMessage(this.messages().challengeDeniedFeedback(
      ChessPlayer.player(challenge.challenger()),
      ChessPlayer.player(challenge.player()),
      challenge.challengerColor()
    ));
  }

  private @Nullable PVPChallenge pollChallenge(final CommandContext<Player> ctx) {
    final Player sender = ctx.sender();
    final @Nullable PVPChallenge challenge = this.boardManager.challenges().getIfPresent(sender.getUniqueId());
    if (challenge == null) {
      ctx.sender().sendMessage(this.messages().noPendingChallenge());
      return null;
    }
    this.boardManager.challenges().invalidate(sender.getUniqueId());
    return challenge;
  }

  private void nextPromotion(final CommandContext<Player> ctx) {
    final Player sender = ctx.sender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    final PieceType type = ctx.get("type");
    board.game().nextPromotion(sender, type);
    sender.sendMessage(this.messages().nextPromotionSet(type));
  }

  private void showLegalMoves(final CommandContext<Player> ctx) {
    final Player player = ctx.sender();
    final boolean hidden = player.getPersistentDataContainer().has(ChessGame.HIDE_LEGAL_MOVES_KEY);
    if (hidden) {
      player.getPersistentDataContainer().remove(ChessGame.HIDE_LEGAL_MOVES_KEY);
    } else {
      player.getPersistentDataContainer().set(ChessGame.HIDE_LEGAL_MOVES_KEY, PersistentDataType.BYTE, (byte) 1);
    }
    player.sendMessage(this.messages().showingLegalMoves(hidden));
  }

  private void deleteBoard(final CommandContext<CommandSender> ctx) {
    final String board = ctx.<ChessBoard>get("board").name();
    this.boardManager.deleteBoard(board);
    ctx.sender().sendMessage(this.messages().boardDeleted(board));
  }

  private void forfeit(final CommandContext<Player> ctx) {
    final Player sender = ctx.sender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    board.game().forfeit(board.game().color(ChessPlayer.player(sender)));
  }

  private void resetBoard(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    if (board.hasGame()) {
      ctx.sender().sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    board.reset(ctx.flags().hasFlag("clear"));
    ctx.sender().sendMessage(this.messages().resetBoard(board));
  }

  private void cpuMatch(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    if (ctx.flags().hasFlag("replace") && board.hasGame() && board.game().cpuVsCpu()) {
      this.cancelCpuMatch(board);
    }
    if (board.hasGame()) {
      ctx.sender().sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    board.startCpuGame(
      ctx.flags().<Integer>getValue("move_delay").orElse(4),
      ctx.flags().<Integer>getValue("white_elo").orElse(1800),
      ctx.flags().<Integer>getValue("black_elo").orElse(2400),
      ctx.flags().getValue("time_control", null)
    );
    final ChessGame game = board.game();
    ctx.sender().sendMessage(this.messages().matchStarted(board, game.white(), game.black()));
  }

  private void cancelMatch(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    if (!board.hasGame()) {
      ctx.sender().sendMessage(this.messages().noMatchToCancel(board.name()));
      return;
    }
    final ChessGame game = board.game();
    board.endGameAndWait();
    game.audience().sendMessage(this.messages().matchCancelled());
    ctx.sender().sendMessage(this.messages().matchCancelled());
  }

  private void pauseMatch(final CommandContext<Player> ctx) {
    final Player sender = ctx.sender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    final ChessPlayer opponent = board.game().player(board.game().color(ChessPlayer.player(sender)).other());
    if (opponent.isCpu()) {
      this.boardManager.pauseMatch(board);
    } else {
      this.boardManager.pauseProposals().put(((ChessPlayer.OnlinePlayer) opponent).uuid(), new Object());
      sender.sendMessage(this.messages().pauseProposedSender(board.game(), board.game().color(ChessPlayer.player(sender))));
      opponent.sendMessage(this.messages().pauseProposedRecipient(board.game(), board.game().color(ChessPlayer.player(sender)).other()));
    }
  }

  private void acceptPause(final CommandContext<Player> ctx) {
    final Player sender = ctx.sender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    final @Nullable Object proposal = this.boardManager.pauseProposals().getIfPresent(sender.getUniqueId());
    if (proposal != null) {
      this.boardManager.pauseProposals().invalidate(sender.getUniqueId());
      this.boardManager.pauseMatch(board);
    } else {
      sender.sendMessage(this.messages().noPauseProposed());
    }
  }

  private CompletableFuture<Void> resumeMatch(final CommandContext<Player> ctx) {
    final ChessBoard board = ctx.get("board");
    final UUID id = ctx.get("id");

    if (this.boardManager.inGame(ctx.sender())) {
      ctx.sender().sendMessage(this.messages().alreadyInGame());
      return CompletableFuture.completedFuture(null);
    } else if (canCancelCpuMatch(board)) {
      this.cancelCpuMatch(board);
    } else if (board.hasGame() || board.autoCpuGame().cpuGamesOnly() || this.boardManager.challenges().asMap().values().stream().anyMatch(c -> c.board() == board)) {
      ctx.sender().sendMessage(this.messages().boardOccupied(board.name()));
      return CompletableFuture.completedFuture(null);
    }

    return this.plugin.database().queryMatch(id).thenAcceptAsync(matchOptional -> {
      if (matchOptional.isEmpty()) {
        ctx.sender().sendMessage(this.messages().noPausedMatch(id));
        return;
      }
      final GameState match = matchOptional.get();
      if (match.result() != null) {
        ctx.sender().sendMessage(this.messages().noPausedMatch(id));
        return;
      }

      final @Nullable PieceColor color = match.color(ctx.sender().getUniqueId());
      if (color == null) {
        throw CommandCompleted.withMessage(this.messages().youAreNotInThisMatch());
      }
      if (!match.cpu(color.other())) {
        final UUID opponentId = match.playerId(color.other());
        final @Nullable Player player = Bukkit.getPlayer(opponentId);
        if (player == null) {
          ctx.sender().sendMessage(this.messages().opponentOffline(
            this.plugin.database().onlineOrCachedPlayer(opponentId).join()
          ));
          return;
        }
        if (this.boardManager.inGame(player)) {
          ctx.sender().sendMessage(this.messages().opponentAlreadyInGame(player));
          return;
        }
        ctx.sender().sendMessage(this.messages().challengeSent(
          color == PieceColor.WHITE ? match.white() : match.black(),
          color == PieceColor.WHITE ? match.black() : match.white(),
          color
        ));
        this.boardManager.challenges().put(opponentId, new PVPChallengeResumeMatchImpl(board, ctx.sender(), player, color.other(), match.timeControlSettings(), match));
        player.sendMessage(this.messages().resumeChallengeReceived(
          color == PieceColor.WHITE ? match.white() : match.black(),
          color == PieceColor.WHITE ? match.black() : match.white(),
          color,
          match.timeControlSettings()
        ));
        return;
      }

      board.resumeGame(match);
    }, this.plugin.getServer().getScheduler().getMainThreadExecutor(this.plugin));
  }

  private record TargetPlayer(UUID uuid, ChessPlayer.Player player) {}

  private CompletableFuture<TargetPlayer> target(final CommandContext<? extends CommandSender> ctx) {
    return ctx.<OfflinePlayer>optional("player")
      .map(offlinePlayer -> this.plugin.database().onlineOrCachedPlayer(offlinePlayer.getUniqueId())
        .thenApply(p -> new TargetPlayer(offlinePlayer.getUniqueId(), p)))
      .orElseGet(() -> {
        if (ctx.sender() instanceof Player p) {
          return CompletableFuture.completedFuture(new TargetPlayer(p.getUniqueId(), ChessPlayer.player(p)));
        } else {
          return CompletableFuture.failedFuture(CommandCompleted.withMessage(this.messages().nonPlayerMustProvidePlayer()));
        }
      });
  }

  private CompletableFuture<Void> pausedMatches(final CommandContext<? extends CommandSender> ctx) {
    return this.target(ctx).thenCompose(target -> {
      return this.plugin.database().queryIncompleteMatches(target.uuid()).thenAcceptAsync(games -> {
        if (games.isEmpty()) {
          ctx.sender().sendMessage(this.messages().noPausedMatches());
          return;
        }

        Pagination.<GameState>builder()
          .header((page, pages) -> this.messages().pausedMatchesHeader(target.player().name(), target.player().displayName()))
          .footer(this.pagination.footerRenderer(commandString(ctx, "/chess paused_matches", target.uuid())))
          .item((item, lastOfPage) -> this.pagination.wrapElement(this.messages().pausedMatchInfo(this.plugin.database(), item, ctx.sender().hasPermission(Permissions.COMMAND_EXPORT_MATCH_INCOMPLETE))))
          .pageOutOfRange(this.pagination.pageOutOfRange())
          .build()
          .render(games, ctx.<Integer>optional("page").orElse(1), 5)
          .forEach(ctx.sender()::sendMessage);
      });
    });
  }

  private CompletableFuture<Void> matchHistory(final CommandContext<? extends CommandSender> ctx) {
    return this.target(ctx).thenCompose(target -> {
      return this.plugin.database().queryCompleteMatches(target.uuid()).thenAcceptAsync(games -> {
        if (games.isEmpty()) {
          ctx.sender().sendMessage(this.messages().noCompleteMatches());
          return;
        }

        final int rating = target.player() instanceof ChessPlayer.CachedPlayer cached
          ? cached.rating()
          : this.plugin.database().queryPlayer(target.uuid()).join().map(ChessPlayer.CachedPlayer::rating).orElse(Elo.INITIAL_RATING);

        Pagination.<GameState>builder()
          .header((page, pages) -> this.messages().matchHistoryHeader(target.player().name(), target.player().displayName(), rating))
          .footer(this.pagination.footerRenderer(commandString(ctx, "/chess match_history", target.uuid())))
          .item((item, lastOfPage) -> this.pagination.wrapElement(this.messages().completeMatchInfo(this.plugin.database(), item, target.uuid(), true)))
          .pageOutOfRange(this.pagination.pageOutOfRange())
          .build()
          .render(games, ctx.<Integer>optional("page").orElse(1), 5)
          .forEach(ctx.sender()::sendMessage);
      });
    });
  }

  private static IntFunction<String> commandString(
    final CommandContext<? extends CommandSender> ctx,
    final String base,
    final UUID player
  ) {
    return page -> {
      final String id;
      if (ctx.sender() instanceof Player sender && sender.getUniqueId().equals(player)) {
        id = "";
      } else {
        id = " " + ctx.parsingContext("player").consumedInput();
      }
      return base + id + " page " + page;
    };
  }

  private CompletableFuture<Void> exportMatch(final CommandContext<Player> ctx) {
    final UUID id = ctx.get("id");
    return this.plugin.database().queryMatch(id).thenAcceptAsync(matchOpt -> matchOpt.ifPresentOrElse(match -> {
      if (!ctx.sender().getUniqueId().equals(match.whiteId()) && !ctx.sender().getUniqueId().equals(match.blackId())) {
        if (!ctx.sender().hasPermission(Permissions.COMMAND_EXPORT_MATCH_OTHERS)) {
          ctx.sender().sendMessage(this.messages().cannotExportOthers());
          return;
        }
      }
      if (match.result() == null && !ctx.sender().hasPermission(Permissions.COMMAND_EXPORT_MATCH_INCOMPLETE)) {
        ctx.sender().sendMessage(this.messages().cannotExportIncomplete());
        return;
      }

      ctx.sender().sendMessage(
        this.messages().clickToCopyPgn()
          .clickEvent(ClickEvent.copyToClipboard(MatchExporter.writePgn(match, this.plugin.database()).join()))
      );
    }, () -> ctx.sender().sendMessage(this.messages().noSuchMatch(id))));
  }

  private CompletableFuture<Void> leaderboard(final CommandContext<CommandSender> ctx) {
    final int limit = 8;
    return this.plugin.database().queryLeaderboard(limit).thenAcceptAsync(leaderboard -> {
      ctx.sender().sendMessage(this.messages().leaderboardHeader());
      for (int i = 0; i < limit; i++) {
        Pair<UUID, Integer> pair;
        try {
          pair = leaderboard.get(i);
          final ChessPlayer player = this.plugin.database().onlineOrCachedPlayer(pair.first()).join();
          leaderboardLine(ctx.sender(), i, player, pair);
        } catch (final IndexOutOfBoundsException ignored) {
          leaderboardLine(ctx.sender(), i, null, null);
        }
      }
    });
  }

  private static void leaderboardLine(
    final CommandSender sender,
    final int i,
    final @Nullable ChessPlayer player,
    final @Nullable Pair<UUID, Integer> pair
  ) {
    final TextComponent.Builder builder = Component.text()
      .append(Component.text(String.format("%2d", i + 1)))
      .append(Component.text(". "));
    if (player != null && pair != null) {
      builder.append(player.displayName())
        .append(Component.text(": "))
        .append(Component.text(pair.second()));
    } else {
      builder.append(Component.text("__________", NamedTextColor.GRAY))
        .append(Component.space());
    }
    sender.sendMessage(builder);
  }

  private @Nullable ChessBoard playerBoard(final Player sender) {
    return this.boardManager.activeBoards().stream()
      .filter(b -> b.game().hasPlayer(sender))
      .findFirst()
      .orElse(null);
  }

  private Messages messages() {
    return this.plugin.config().messages();
  }

  private static LegacyPaperCommandManager<CommandSender> createCommandManager(final ChessCraft plugin) {
    final LegacyPaperCommandManager<CommandSender> mgr =
      LegacyPaperCommandManager.createNative(plugin, ExecutionCoordinator.simpleCoordinator());
    mgr.registerCommandPreProcessor(ctx -> ctx.commandContext().set(PLUGIN, plugin));
    mgr.registerBrigadier();

    final BukkitBrigadierMapper<CommandSender> brigMapper = new BukkitBrigadierMapper<>(plugin.getLogger(), mgr.brigadierManager());
    brigMapper.mapSimpleNMS(TypeToken.get(TimeControlParser.class), "resource_location", true);

    MinecraftExceptionHandler.<CommandSender>createNative()
      .defaultHandlers()
      .captionFormatter(ComponentCaptionFormatter.miniMessage())
      .registerTo(mgr);

    mgr.exceptionController()
      .registerHandler(CommandExecutionException.class, unwrappingHandler(CommandCompleted.class))
      .registerHandler(CommandCompleted.class, ctx -> {
        final @Nullable Component message = ctx.exception().componentMessage();
        if (message != null) {
          ctx.context().sender().sendMessage(message);
        }
      });

    final LocaleExtractor<CommandSender> extractor = audienceLocaleExtractor();

    mgr.captionRegistry()
      .registerProvider(core(extractor))
      .registerProvider(bukkit(extractor))
      .registerProvider(minecraftExtras(extractor));

    return mgr;
  }
}
