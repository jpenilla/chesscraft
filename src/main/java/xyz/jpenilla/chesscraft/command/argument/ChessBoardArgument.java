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
package xyz.jpenilla.chesscraft.command.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

@DefaultQualifier(NonNull.class)
public final class ChessBoardArgument extends CommandArgument<CommandSender, ChessBoard> {
  ChessBoardArgument(
    final boolean required,
    final String name,
    final String defaultValue,
    final @Nullable BiFunction<CommandContext<CommandSender>, String, List<String>> suggestionsProvider,
    final ArgumentDescription defaultDescription,
    final SuggestionsMode suggestions
  ) {
    super(required, name, new Parser(suggestions), defaultValue, ChessBoard.class, suggestionsProvider, defaultDescription);
  }

  public static Builder builder(final String name) {
    return new Builder(name);
  }

  public static ChessBoardArgument create(final String name) {
    return builder(name).asRequired().build();
  }

  public static final class Builder extends CommandArgument.TypedBuilder<CommandSender, ChessBoard, Builder> {
    private SuggestionsMode suggestions = SuggestionsMode.ALL;

    private Builder(final String name) {
      super(ChessBoard.class, name);
    }

    public Builder suggestions(final SuggestionsMode mode) {
      this.suggestions = mode;
      return this;
    }

    public Builder onlySuggestPlayable() {
      return this.suggestions(SuggestionsMode.PLAYABLE_ONLY);
    }

    public Builder onlySuggestOccupied() {
      return this.suggestions(SuggestionsMode.OCCUPIED_ONLY);
    }

    @Override
    public ChessBoardArgument build() {
      return new ChessBoardArgument(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription(),
        this.suggestions
      );
    }
  }

  public static final class Parser implements ArgumentParser<CommandSender, ChessBoard> {
    private final SuggestionsMode suggestions;

    public Parser(final SuggestionsMode suggestions) {
      this.suggestions = suggestions;
    }

    @Override
    public ArgumentParseResult<ChessBoard> parse(
      final CommandContext<CommandSender> commandContext,
      final Queue<String> inputQueue
    ) {
      final ChessCraft plugin = commandContext.get(Commands.PLUGIN);
      final ChessBoard g = plugin.boardManager().board(inputQueue.peek());
      if (g != null) {
        inputQueue.poll();
        return ArgumentParseResult.success(g);
      }
      return ArgumentParseResult.failure(
        ComponentRuntimeException.withMessage(plugin.config().messages().noSuchBoard(inputQueue.peek())));
    }

    @Override
    public List<String> suggestions(
      final CommandContext<CommandSender> commandContext,
      final String input
    ) {
      final ChessCraft plugin = commandContext.get(Commands.PLUGIN);
      return plugin.boardManager().boards().stream()
        .filter(board -> switch (this.suggestions) {
          case ALL -> true;
          case PLAYABLE_ONLY -> {
            if (board.autoCpuGame().cpuGamesOnly()) {
              yield false;
            }
            yield !board.hasGame() || board.game().cpuVsCpu() && board.autoCpuGame().enabled;
          }
          case OCCUPIED_ONLY -> board.hasGame();
        })
        .map(ChessBoard::name)
        .toList();
    }
  }

  public enum SuggestionsMode {
    PLAYABLE_ONLY,
    OCCUPIED_ONLY,
    ALL
  }
}
