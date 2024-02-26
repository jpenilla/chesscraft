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
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.internal.BukkitBrigadierMapper;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.flag.FlagContext;
import org.incendo.cloud.translations.LocaleExtractor;
import xyz.jpenilla.chesscraft.BoardManager;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.ChessGame;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.command.parser.ChessBoardParser;
import xyz.jpenilla.chesscraft.command.parser.TimeControlParser;
import xyz.jpenilla.chesscraft.config.Messages;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.PVPChallenge;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

import static org.incendo.cloud.bukkit.parser.MaterialParser.materialParser;
import static org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser.singlePlayerSelectorParser;
import static org.incendo.cloud.exception.handling.ExceptionHandler.unwrappingHandler;
import static org.incendo.cloud.key.CloudKey.cloudKey;
import static org.incendo.cloud.parser.standard.EnumParser.enumParser;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static org.incendo.cloud.translations.TranslationBundle.core;
import static org.incendo.cloud.translations.bukkit.BukkitTranslationBundle.bukkit;
import static org.incendo.cloud.translations.minecraft.extras.AudienceLocaleExtractor.audienceLocaleExtractor;
import static org.incendo.cloud.translations.minecraft.extras.MinecraftExtrasTranslationBundle.minecraftExtras;
import static xyz.jpenilla.chesscraft.command.parser.ChessBoardParser.chessBoardParser;
import static xyz.jpenilla.chesscraft.command.parser.PromotionParser.promotionParser;
import static xyz.jpenilla.chesscraft.command.parser.TimeControlParser.timeControlParser;

public final class Commands {
  public static final CloudKey<ChessCraft> PLUGIN = cloudKey("chesscraft", ChessCraft.class);

  private final ChessCraft plugin;
  private final PaperCommandManager<CommandSender> mgr;
  private final BoardManager boardManager;

  public Commands(final ChessCraft plugin) {
    this.plugin = plugin;
    this.boardManager = plugin.boardManager();
    this.mgr = createCommandManager(plugin);
  }

  public void register() {
    this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, this.boardManager.challenges()::cleanUp, 20L, 20L * 60L);

    new HelpCommand(this.mgr).register();

    final Command.Builder<CommandSender> chess = this.mgr.commandBuilder("chess");

    this.mgr.command(chess.literal("version")
      .permission("chesscraft.command.version")
      .handler(this::version));

    this.mgr.command(chess.literal("boards")
      .permission("chesscraft.command.boards")
      .handler(this::boards));

    this.mgr.command(chess.literal("reload")
      .permission("chesscraft.command.reload")
      .handler(this::reload));

    this.mgr.command(chess.literal("create_board")
      .required("name", stringParser())
      .required("facing", enumParser(CardinalDirection.class))
      .optional("scale", integerParser(1), DefaultValue.constant(1))
      .senderType(Player.class)
      .permission("chesscraft.command.create_board")
      .handler(this::createBoard));

    this.mgr.command(chess.literal("set_checkerboard")
      .required("board", chessBoardParser())
      .flag(CommandFlag.builder("black").withComponent(materialParser()))
      .flag(CommandFlag.builder("white").withComponent(materialParser()))
      .flag(CommandFlag.builder("border").withComponent(materialParser()))
      .permission("chesscraft.command.set_checkerboard")
      .handler(this::setCheckerboard));

    this.mgr.command(chess.literal("delete_board")
      .required("board", chessBoardParser())
      .permission("chesscraft.command.delete_board")
      .handler(this::deleteBoard));

    this.mgr.command(chess.literal("challenge")
      .literal("cpu")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.PLAYABLE_ONLY))
      .required("color", enumParser(PieceColor.class))
      .required("cpu_elo", integerParser(100, 4000))
      .optional("time_control", timeControlParser())
      .senderType(Player.class)
      .permission("chesscraft.command.challenge.cpu")
      .handler(this::challengeCpu));

    this.mgr.command(chess.literal("challenge")
      .literal("player")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.PLAYABLE_ONLY))
      .required("player", singlePlayerSelectorParser())
      .required("color", enumParser(PieceColor.class))
      .optional("time_control", timeControlParser())
      .senderType(Player.class)
      .permission("chesscraft.command.challenge.player")
      .handler(this::challengePlayer));

    this.mgr.command(chess.literal("accept")
      .senderType(Player.class)
      .permission("chesscraft.command.accept")
      .handler(this::accept));

    this.mgr.command(chess.literal("deny")
      .senderType(Player.class)
      .permission("chesscraft.command.deny")
      .handler(this::deny));

    this.mgr.command(chess.literal("next_promotion")
      .required("type", promotionParser())
      .senderType(Player.class)
      .permission("chesscraft.command.next_promotion")
      .handler(this::nextPromotion));

    this.mgr.command(chess.literal("show_legal_moves")
      .senderType(Player.class)
      .permission("chesscraft.command.show_legal_moves")
      .handler(this::showLegalMoves));

    this.mgr.command(chess.literal("forfeit")
      .senderType(Player.class)
      .permission("chesscraft.command.forfeit")
      .handler(this::forfeit));

    this.mgr.command(chess.literal("reset_board")
      .required("board", chessBoardParser())
      .flag(CommandFlag.builder("clear"))
      .permission("chesscraft.command.reset_board")
      .handler(this::resetBoard));

    this.mgr.command(chess.literal("cpu_match")
      .required("board", chessBoardParser())
      .flag(CommandFlag.builder("white_elo").withAliases("w").withComponent(integerParser(100, 4000)))
      .flag(CommandFlag.builder("black_elo").withAliases("b").withComponent(integerParser(100, 4000)))
      .flag(CommandFlag.builder("move_delay").withAliases("d").withComponent(integerParser(0)))
      .flag(CommandFlag.builder("time_control").withAliases("t").withComponent(timeControlParser()))
      .flag(CommandFlag.builder("replace").withAliases("r"))
      .permission("chesscraft.command.cpu_match")
      .handler(this::cpuMatch));

    this.mgr.command(chess.literal("cancel_match")
      .required("board", chessBoardParser(ChessBoardParser.SuggestionsMode.OCCUPIED_ONLY))
      .permission("chesscraft.command.cancel_game")
      .handler(this::cancelMatch));
  }

  private void version(final CommandContext<CommandSender> ctx) {
    ctx.sender().sendRichMessage("<bold><black>Chess<white>Craft");
    ctx.sender().sendRichMessage("<gray><italic>  v" + this.plugin.getDescription().getVersion());
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
    ctx.sender().sendRichMessage("<gray><italic>Reloading configs... This will end any active matches.");
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
      new PVPChallenge(board, sender, opponent, userColor, timeControl)
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

    final ChessPlayer senderPlayer = ChessPlayer.player(challenge.challenger());
    final ChessPlayer opp = ChessPlayer.player(challenge.player());
    challenge.board().startGame(
      challenge.challengerColor() == PieceColor.WHITE ? senderPlayer : opp,
      challenge.challengerColor() == PieceColor.BLACK ? senderPlayer : opp,
      challenge.timeControl()
    );
  }

  private void deny(final CommandContext<Player> ctx) {
    final @Nullable PVPChallenge challenge = this.pollChallenge(ctx);
    if (challenge == null) {
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

  private @Nullable ChessBoard playerBoard(final Player sender) {
    return this.boardManager.activeBoards().stream()
      .filter(b -> b.game().hasPlayer(sender))
      .findFirst()
      .orElse(null);
  }

  private Messages messages() {
    return this.plugin.config().messages();
  }

  private static PaperCommandManager<CommandSender> createCommandManager(final ChessCraft plugin) {
    final PaperCommandManager<CommandSender> mgr =
      PaperCommandManager.createNative(plugin, ExecutionCoordinator.simpleCoordinator());
    mgr.registerCommandPreProcessor(ctx -> ctx.commandContext().set(PLUGIN, plugin));
    mgr.registerBrigadier();

    final BukkitBrigadierMapper<CommandSender> brigMapper = new BukkitBrigadierMapper<>(mgr, mgr.brigadierManager());
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
