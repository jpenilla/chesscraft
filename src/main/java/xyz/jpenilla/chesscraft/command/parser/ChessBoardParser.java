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
package xyz.jpenilla.chesscraft.command.parser;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserDescriptor;
import cloud.commandframework.arguments.suggestion.BlockingSuggestionProvider;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandInput;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

@DefaultQualifier(NonNull.class)
public final class ChessBoardParser implements ArgumentParser<CommandSender, ChessBoard>, BlockingSuggestionProvider.Strings<CommandSender> {
  private final SuggestionsMode suggestions;

  private ChessBoardParser(final SuggestionsMode suggestions) {
    this.suggestions = suggestions;
  }

  public static ParserDescriptor<CommandSender, ChessBoard> chessBoardParser() {
    return chessBoardParser(SuggestionsMode.ALL);
  }

  public static ParserDescriptor<CommandSender, ChessBoard> chessBoardParser(final SuggestionsMode suggestionsMode) {
    return ParserDescriptor.of(new ChessBoardParser(suggestionsMode), ChessBoard.class);
  }

  @Override
  public ArgumentParseResult<ChessBoard> parse(
    final CommandContext<CommandSender> commandContext,
    final CommandInput commandInput
  ) {
    final ChessCraft plugin = commandContext.get(Commands.PLUGIN);
    final String input = commandInput.readString();
    final ChessBoard g = plugin.boardManager().board(input);
    if (g != null) {
      return ArgumentParseResult.success(g);
    }
    return ArgumentParseResult.failure(
      ComponentRuntimeException.withMessage(plugin.config().messages().noSuchBoard(input)));
  }

  @Override
  public List<String> stringSuggestions(
    final CommandContext<CommandSender> commandContext,
    final CommandInput input
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

  public enum SuggestionsMode {
    PLAYABLE_ONLY,
    OCCUPIED_ONLY,
    ALL
  }
}
