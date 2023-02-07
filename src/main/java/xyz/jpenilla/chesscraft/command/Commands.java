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

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.flags.FlagContext;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.BoardManager;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.config.Messages;
import xyz.jpenilla.chesscraft.data.PVPChallenge;
import xyz.jpenilla.chesscraft.data.Vec3;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

public final class Commands {
  private static final Set<PieceType> VALID_PROMOTIONS = Set.of(PieceType.BISHOP, PieceType.KNIGHT, PieceType.QUEEN, PieceType.ROOK);

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
      .argument(StringArgument.single("name"))
      .senderType(Player.class)
      .permission("chesscraft.command.create_board")
      .handler(this::createBoard));

    this.mgr.command(chess.literal("set_checkerboard")
      .argument(this.boardArgument("board"))
      .flag(this.mgr.flagBuilder("black").withArgument(MaterialArgument.of("material")))
      .flag(this.mgr.flagBuilder("white").withArgument(MaterialArgument.of("material")))
      .flag(this.mgr.flagBuilder("border").withArgument(MaterialArgument.of("material")))
      .permission("chesscraft.command.set_checkerboard")
      .handler(this::setCheckerboard));

    this.mgr.command(chess.literal("delete_board")
      .argument(this.boardArgument("board"))
      .permission("chesscraft.command.delete_board")
      .handler(this::deleteBoard));

    this.mgr.command(chess.literal("challenge")
      .literal("cpu")
      .argument(this.boardArgument("board"))
      .argument(EnumArgument.of(PieceColor.class, "color"))
      .argument(IntegerArgument.<CommandSender>builder("cpu_elo").withMin(100).withMax(4000).asOptional())
      .senderType(Player.class)
      .permission("chesscraft.command.challenge.cpu")
      .handler(this::challengeCpu));

    this.mgr.command(chess.literal("challenge")
      .literal("player")
      .argument(this.boardArgument("board"))
      .argument(SinglePlayerSelectorArgument.of("player"))
      .argument(EnumArgument.of(PieceColor.class, "color"))
      .senderType(Player.class)
      .permission("chesscraft.command.challenge.player")
      .handler(this::challengePlayer));

    this.mgr.command(chess.literal("accept")
      .senderType(Player.class)
      .permission("chesscraft.command.accept")
      .handler(this::accept));

    this.mgr.command(chess.literal("next_promotion")
      .argument(this.promotionArgument("type"))
      .senderType(Player.class)
      .permission("chesscraft.command.next_promotion")
      .handler(this::nextPromotion));

    this.mgr.command(chess.literal("forfeit")
      .senderType(Player.class)
      .permission("chesscraft.command.forfeit")
      .handler(this::forfeit));
  }

  private void version(final CommandContext<CommandSender> ctx) {
    ctx.getSender().sendRichMessage("<bold><black>Chess<white>Craft");
    ctx.getSender().sendRichMessage("<gray><italic>  v" + this.plugin.getDescription().getVersion());
  }

  private void boards(final CommandContext<CommandSender> ctx) {
    final CommandSender sender = ctx.getSender();
    sender.sendMessage("Chess boards:");
    for (final ChessBoard board : this.boardManager.boards()) {
      ctx.getSender().sendMessage(Component.text().content(board.name()).apply(builder -> {
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
    this.plugin.reloadMainConfig();
    ctx.getSender().sendRichMessage("<gray><italic>Reloading configs... This will end any active matches.");
    this.boardManager.reload();
    ctx.getSender().sendRichMessage("<green>Reloaded configs.");
  }

  private void createBoard(final CommandContext<CommandSender> ctx) {
    final String name = ctx.get("name");
    final Player sender = (Player) ctx.getSender();
    if (this.boardManager.board(name) != null) {
      sender.sendMessage(this.messages().boardAlreadyExists(name));
      return;
    }
    this.boardManager.createBoard(
      name,
      sender.getWorld(),
      Vec3.fromLocation(sender.getLocation())
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
    ctx.getSender().sendRichMessage("<green>Set blocks to world!");
  }

  private void challengeCpu(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    final Player sender = (Player) ctx.getSender();
    if (this.boardManager.inGame(sender)) {
      sender.sendMessage(this.messages().alreadyInGame());
      return;
    }
    if (board.hasGame()) {
      sender.sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    final PieceColor userColor = ctx.get("color");
    final ChessPlayer user = ChessPlayer.player(sender);
    board.startGame(
      userColor == PieceColor.WHITE ? user : ChessPlayer.CPU,
      userColor == PieceColor.BLACK ? user : ChessPlayer.CPU,
      ctx.getOrDefault("cpu_elo", 800)
    );
  }

  private void challengePlayer(final CommandContext<CommandSender> ctx) {
    final ChessBoard board = ctx.get("board");
    final Player sender = (Player) ctx.getSender();
    final Player opponent = Objects.requireNonNull(ctx.<SinglePlayerSelector>get("player").getPlayer());
    if (sender.equals(opponent)) {
      ctx.getSender().sendRichMessage("<red>You cannot challenge yourself.");
      return;
    } else if (this.boardManager.inGame(sender)) {
      sender.sendMessage(this.messages().alreadyInGame());
      return;
    } else if (this.boardManager.inGame(opponent)) {
      sender.sendMessage(this.messages().opponentAlreadyInGame(opponent));
      return;
    } else if (board.hasGame() || this.boardManager.challenges().asMap().values().stream().anyMatch(c -> c.board() == board)) {
      sender.sendMessage(this.messages().boardOccupied(board.name()));
      return;
    }
    final PieceColor userColor = ctx.get("color");
    final ChessPlayer user = ChessPlayer.player(sender);
    final ChessPlayer opp = ChessPlayer.player(opponent);
    this.boardManager.challenges().put(opponent.getUniqueId(), new PVPChallenge(board, sender, opponent, userColor));
    sender.sendMessage(this.messages().challengeSent(user, opp, userColor));
    opponent.sendMessage(this.messages().challengeReceived(user, opp, userColor));
  }

  private void accept(final CommandContext<CommandSender> ctx) {
    final Player sender = (Player) ctx.getSender();
    final @Nullable PVPChallenge challenge = this.boardManager.challenges().getIfPresent(sender.getUniqueId());
    if (challenge == null) {
      ctx.getSender().sendMessage(this.messages().noChallengeToAccept());
      return;
    }
    this.boardManager.challenges().invalidate(sender.getUniqueId());
    if (this.boardManager.inGame(challenge.challenger())) {
      sender.sendMessage(this.messages().opponentAlreadyInGame(challenge.challenger()));
      return;
    }

    final ChessPlayer senderPlayer = ChessPlayer.player(challenge.challenger());
    final ChessPlayer opp = ChessPlayer.player(challenge.player());
    challenge.board().startGame(
      challenge.challengerColor() == PieceColor.WHITE ? senderPlayer : opp,
      challenge.challengerColor() == PieceColor.BLACK ? senderPlayer : opp
    );
  }

  private void nextPromotion(final CommandContext<CommandSender> ctx) {
    final Player sender = (Player) ctx.getSender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    final PieceType type = ctx.get("type");
    board.game().nextPromotion(sender, type);
    sender.sendMessage(this.messages().nextPromotionSet(type));
  }

  private void deleteBoard(final CommandContext<CommandSender> ctx) {
    final String board = ctx.<ChessBoard>get("board").name();
    this.boardManager.deleteBoard(board);
    ctx.getSender().sendMessage(this.messages().boardDeleted(board));
  }

  private void forfeit(final CommandContext<CommandSender> ctx) {
    final Player sender = (Player) ctx.getSender();
    final ChessBoard board = this.playerBoard(sender);
    if (board == null) {
      sender.sendMessage(this.messages().mustBeInMatch());
      return;
    }
    board.game().forfeit(board.game().color(ChessPlayer.player(sender)));
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

  private CommandArgument<CommandSender, PieceType> promotionArgument(final String name) {
    return this.mgr.argumentBuilder(PieceType.class, name)
      .withParser(new EnumArgument.EnumParser<CommandSender, PieceType>(PieceType.class).map((ctx, type) -> {
        if (VALID_PROMOTIONS.contains(type)) {
          return ArgumentParseResult.success(type);
        } else {
          return ArgumentParseResult.failure(new IllegalArgumentException());
        }
      }))
      .withSuggestionsProvider((sender, input) -> VALID_PROMOTIONS.stream().map(Enum::name).toList())
      .build();
  }

  private CommandArgument<CommandSender, ChessBoard> boardArgument(final String name) {
    return this.mgr.argumentBuilder(ChessBoard.class, name)
      .withParser((sender, queue) -> {
        final ChessBoard g = this.boardManager.board(queue.peek());
        if (g != null) {
          queue.poll();
          return ArgumentParseResult.success(g);
        }
        return ArgumentParseResult.failure(
          ComponentRuntimeException.withMessage(this.messages().noSuchBoard(queue.peek())));
      })
      .withSuggestionsProvider((sender, input) -> this.boardManager.boards().stream().map(ChessBoard::name).toList())
      .build();
  }

  private static PaperCommandManager<CommandSender> createCommandManager(final ChessCraft plugin) {
    final PaperCommandManager<CommandSender> mgr;
    try {
      mgr = PaperCommandManager.createNative(plugin, CommandExecutionCoordinator.simpleCoordinator());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    mgr.registerBrigadier();
    final CloudBrigadierManager<CommandSender, ?> brigMgr = Objects.requireNonNull(mgr.brigadierManager());
    brigMgr.setNativeNumberSuggestions(false);
    new MinecraftExceptionHandler<CommandSender>()
      .withDefaultHandlers()
      .apply(mgr, AudienceProvider.nativeAudience());
    final BiConsumer<CommandSender, CommandExecutionException> handler = Objects.requireNonNull(mgr.getExceptionHandler(CommandExecutionException.class));
    mgr.registerExceptionHandler(CommandExecutionException.class, (sender, ex) -> {
      if (ex.getCause() instanceof CommandCompleted completed) {
        final @Nullable Component message = completed.componentMessage();
        if (message != null) {
          sender.sendMessage(message);
        }
        return;
      }
      handler.accept(sender, ex);
    });
    return mgr;
  }
}
