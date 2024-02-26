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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

@DefaultQualifier(NonNull.class)
public final class PromotionParser implements ArgumentParser<CommandSender, PieceType>, BlockingSuggestionProvider.Strings<CommandSender> {
  public static final Set<PieceType> VALID_PROMOTIONS = Set.of(PieceType.BISHOP, PieceType.KNIGHT, PieceType.QUEEN, PieceType.ROOK);

  public static ParserDescriptor<CommandSender, PieceType> promotionParser() {
    return ParserDescriptor.of(new PromotionParser(), PieceType.class);
  }

  private static final List<String> SUGGESTIONS = VALID_PROMOTIONS.stream().map(PieceType::name).toList();

  @Override
  public ArgumentParseResult<PieceType> parse(
    final CommandContext<CommandSender> commandContext,
    final CommandInput commandInput
  ) {
    final String input = commandInput.readString();

    for (final PieceType value : VALID_PROMOTIONS) {
      if (value.name().equalsIgnoreCase(input)) {
        return ArgumentParseResult.success(value);
      }
    }

    return ArgumentParseResult.failure(new ParseException(input, commandContext));
  }

  @Override
  public List<String> stringSuggestions(
    final CommandContext<CommandSender> commandContext,
    final CommandInput commandInput
  ) {
    return SUGGESTIONS;
  }

  public static final class ParseException extends ParserException {
    private static final long serialVersionUID = 2261721931860121220L;

    public ParseException(
      final String input,
      final CommandContext<?> context
    ) {
      super(
        EnumParser.class,
        context,
        StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_ENUM,
        CaptionVariable.of("input", input),
        CaptionVariable.of("acceptableValues", acceptableValues())
      );
    }

    private static String acceptableValues() {
      return VALID_PROMOTIONS.stream()
        .map(e -> e.toString().toLowerCase())
        .collect(Collectors.joining(", "));
    }
  }
}
